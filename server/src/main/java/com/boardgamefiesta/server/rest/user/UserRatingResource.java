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

package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.server.auth.Roles;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/users/{userId}/ratings")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
public class UserRatingResource {

    @Inject
    Ratings ratings;

    @Inject
    Users users;

    @GET
    public List<RatingView> getRatings(@PathParam("userId") String userId,
                                       @QueryParam("gameId") String gameId,
                                       @QueryParam("tableId") String tableId) {
        if (tableId != null && !"".equals(tableId)) {
            return ratings.findByTable(User.Id.of(userId), Table.Id.of(tableId))
                    .map(rating -> {
                        var userMap = rating.getDeltas().keySet().stream()
                                .flatMap(deltaUserId -> users.findById(deltaUserId).stream())
                                .collect(Collectors.toMap(User::getId, Function.identity()));
                        return new RatingView(rating, userMap);
                    })
                    .map(Collections::singletonList)
                    .orElseThrow(NotFoundException::new);
        } else {
            return ratings.findHistoric(User.Id.of(userId), Game.Id.of(gameId),
                    Instant.now().minus(365, ChronoUnit.DAYS),
                    Instant.now())
                    .map(rating -> new RatingView(rating, Collections.emptyMap()))
                    .collect(Collectors.toList());
        }
    }

}
