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
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.server.rest.CurrentUser;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class GamesResource {

    @Inject
    Games games;

    @Inject
    FeatureToggles featureToggles;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<GameView> get() {
        var currentUserId = currentUser.getOptionalId();

        return games.list()
                .filter(game -> FeatureToggle.Id.forGameId(game.getId())
                        .map(featureToggleId -> featureToggles.findById(featureToggleId)
                                .map(featureToggle -> currentUserId.map(featureToggle::isEnabled).orElse(false))
                                .orElse(false))
                        .orElse(true))
                .sorted(Comparator.comparing(game -> game.getId().getId()))
                .map(GameView::new)
                .collect(Collectors.toList());
    }

}
