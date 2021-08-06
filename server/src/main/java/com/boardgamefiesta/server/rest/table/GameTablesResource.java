/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.server.rest.table;

import com.boardgamefiesta.domain.featuretoggle.FeatureToggle;
import com.boardgamefiesta.domain.featuretoggle.FeatureToggles;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/games/{gameId}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
@Slf4j
public class GameTablesResource {

    private static final int MAX_RESULTS = 20;

    private final FeatureToggles featureToggles;
    private final Tables tables;
    private final Users users;
    private final CurrentUser currentUser;

    @Inject
    public GameTablesResource(@NonNull FeatureToggles featureToggles,
                              @NonNull Tables tables,
                              @NonNull Users users,
                              @NonNull CurrentUser currentUser) {
        this.featureToggles = featureToggles;
        this.tables = tables;
        this.users = users;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/started")
    public List<TableView> getStarted(@PathParam("gameId") Game.Id gameId,
                                      @QueryParam("lts") String lts,
                                      @QueryParam("lid") String lid) {
        var currentUserId = currentUser.getId();

        checkFeatureToggle(gameId);

        var results = (lts != null && !"".equals(lts.trim()) && lid != null && !"".equals(lid.trim())
                ? tables.findStarted(gameId, MAX_RESULTS, Tables.MIN_TIMESTAMP, Instant.parse(lts.trim()), Table.Id.of(lid.trim()))
                : tables.findStarted(gameId, MAX_RESULTS, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP))
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
    public List<TableView> getOpen(@PathParam("gameId") Game.Id gameId,
                                   @QueryParam("lts") String lts,
                                   @QueryParam("lid") String lid) {
        var currentUserId = currentUser.getId();

        checkFeatureToggle(gameId);

        var results = (lts != null && !"".equals(lts.trim()) && lid != null && !"".equals(lid.trim())
                ? tables.findOpen(gameId, MAX_RESULTS, Tables.MIN_TIMESTAMP, Instant.parse(lts.trim()), Table.Id.of(lid.trim()))
                : tables.findOpen(gameId, MAX_RESULTS, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP))
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

    private void checkFeatureToggle(Game.Id gameId) {
        FeatureToggle.Id.forGameId(gameId)
                .map(featureToggles::get)
                .ifPresent(featureToggle -> featureToggle.throwIfNotContains(currentUser.getId()));
    }
}
