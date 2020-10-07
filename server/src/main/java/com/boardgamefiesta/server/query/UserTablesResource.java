package com.boardgamefiesta.server.query;

import com.boardgamefiesta.server.domain.game.Games;
import com.boardgamefiesta.server.domain.rating.Rating;
import com.boardgamefiesta.server.domain.rating.Ratings;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.table.Tables;
import com.boardgamefiesta.server.domain.user.User;
import com.boardgamefiesta.server.domain.user.Users;
import com.boardgamefiesta.server.rest.table.view.TableView;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/users/{userId}/tables")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Slf4j
@Transactional
public class UserTablesResource {

    @Inject
    Games games;

    @Inject
    Tables tables;

    @Inject
    Users users;

    @Inject
    Ratings ratings;

    @Context
    SecurityContext securityContext;

    @GET
    public List<TableView> getTables(@PathParam("userId") String userId) {
        var currentUserId = currentUserId();

        var userMap = new HashMap<User.Id, User>();
        var ratingMap = new HashMap<User.Id, Rating>();
        return tables.findRecent(User.Id.of(userId), 10)
                .map(table -> new TableView(table, getUserMap(table, userMap), getRatingMap(table, ratingMap), currentUserId))
                .collect(Collectors.toList());
    }

    private User.Id currentUserId() {
        if (securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("");
        }
        return User.Id.of(securityContext.getUserPrincipal().getName());
    }

    private Map<User.Id, User> getUserMap(Table table, Map<User.Id, User> userMap) {
        table.getPlayers().forEach(player -> player.getUserId().ifPresent(userId ->
                userMap.computeIfAbsent(userId, k -> users.findOptionallyById(userId).orElse(null))));
        return userMap;
    }

    private Map<User.Id, Rating> getRatingMap(Table table, Map<User.Id, Rating> ratingMap) {
        table.getPlayers().forEach(player -> player.getUserId().ifPresent(userId ->
                ratingMap.computeIfAbsent(userId, k -> ratings.findLatest(userId, table.getGame().getId()))));
        return ratingMap;
    }
}
