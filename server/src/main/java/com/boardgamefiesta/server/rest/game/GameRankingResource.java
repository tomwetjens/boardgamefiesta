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

package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.domain.featuretoggle.FeatureToggle;
import com.boardgamefiesta.domain.featuretoggle.FeatureToggles;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/games/{gameId}/ranking")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
@Slf4j
public class GameRankingResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    FeatureToggles featureToggles;

    @Inject
    Ratings ratings;

    @Inject
    Users users;

    @GET
    public List<RankingView> getRanking(@PathParam("gameId") Game.Id gameId) {
        checkFeatureToggle(gameId);

        return ratings.findRanking(gameId, 10)
                .flatMap(ranking -> users.findById(ranking.getUserId())
                        .map(user -> new RankingView(user, ranking.getRating()))
                        .stream())
                .collect(Collectors.toList());
    }

    private void checkFeatureToggle(Game.Id gameId) {
        FeatureToggle.Id.forGameId(gameId)
                .map(featureToggles::get)
                .ifPresent(featureToggle -> featureToggle.throwIfNotContains(currentUser.getId()));
    }

}
