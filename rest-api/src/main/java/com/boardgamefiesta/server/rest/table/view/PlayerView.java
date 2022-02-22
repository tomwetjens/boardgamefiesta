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

package com.boardgamefiesta.server.rest.table.view;

import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

@Value
public class PlayerView {

    String id;
    Player.Type type;
    UserView user;
    Player.Status status;
    Integer rating;
    Instant turnLimit;
    boolean canKickAfterTurnLimit;
    Integer score;
    Boolean winner;
    PlayerColor color;

    PlayerView(@NonNull Player player,
               @NonNull Function<User.Id, User> userFunction,
               @NonNull Map<User.Id, Rating> ratingMap) {
        id = player.getId().getId();
        type = player.getType();
        status = player.getStatus();
        rating = player.getUserId().map(ratingMap::get).map(Rating::getRating).orElse(null);
        this.user = player.getUserId()
                .map(userId ->
                        new UserView(userId, userFunction.apply(userId), null))
                .orElse(null);
        turnLimit = player.getTurnLimit().orElse(null);
        canKickAfterTurnLimit = player.canKickAfterTurnLimit();
        score = player.getScore().orElse(null);
        winner = player.getWinner().orElse(null);
        color = player.getColor().orElse(null);
    }

}
