package com.boardgamefiesta.server.rest.table;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.table.view.TableView;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/games/{gameId}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
@Slf4j
public class GameTablesResource {

    private static final int MAX_RESULTS = 20;

    private final Tables tables;
    private final Users users;
    private final CurrentUser currentUser;

    @Inject
    public GameTablesResource(@NonNull Tables tables,
                              @NonNull Users users,
                              @NonNull CurrentUser currentUser) {
        this.tables = tables;
        this.users = users;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/started")
    public List<TableView> getStarted(@PathParam("gameId") String gameId,
                                      @QueryParam("lts") String lts,
                                      @QueryParam("lid") String lid) {
        var currentUserId = currentUser.getId();

        var results = (lts != null && !"".equals(lts.trim()) && lid != null && !"".equals(lid.trim())
                ? tables.findStarted(Game.Id.of(gameId), MAX_RESULTS, Tables.MIN_TIMESTAMP, Instant.parse(lts.trim()), Table.Id.of(lid.trim()))
                : tables.findStarted(Game.Id.of(gameId), MAX_RESULTS, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP))
                .collect(Collectors.toList());

        var userMap = users.findByIds(results.stream()
                .map(Table::getPlayers)
                .flatMap(Set::stream)
                .map(Player::getUserId)
                .flatMap(Optional::stream)
                .distinct())
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return results
                .stream()
                .map(table -> new TableView(table, userMap, Collections.emptyMap(), currentUserId))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/open")
    public List<TableView> getOpen(@PathParam("gameId") String gameId,
                                   @QueryParam("lts") String lts,
                                   @QueryParam("lid") String lid) {
        var currentUserId = currentUser.getId();

        var results = (lts != null && !"".equals(lts.trim()) && lid != null && !"".equals(lid.trim())
                ? tables.findOpen(Game.Id.of(gameId), MAX_RESULTS, Tables.MIN_TIMESTAMP, Instant.parse(lts.trim()), Table.Id.of(lid.trim()))
                : tables.findOpen(Game.Id.of(gameId), MAX_RESULTS, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP))
                .filter(table -> table.canJoin(currentUserId))
                .collect(Collectors.toList());

        var userMap = users.findByIds(results.stream()
                .map(Table::getPlayers)
                .flatMap(Set::stream)
                .map(Player::getUserId)
                .flatMap(Optional::stream)
                .distinct())
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return results
                .stream()
                .map(table -> new TableView(table, userMap, Collections.emptyMap(), currentUserId))
                .collect(Collectors.toList());
    }

}
