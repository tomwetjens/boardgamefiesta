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

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
public class RatingView {

    String userId;
    String gameId;
    String tableId;
    Instant timestamp;
    float rating;

    List<DeltaView> deltas;

    RatingView(@NonNull Rating rating, Map<User.Id, User> userMap) {
        this.userId = rating.getUserId().getId();
        this.gameId = rating.getGameId().getId();
        this.tableId = rating.getTableId().map(Table.Id::getId).orElse(null);
        this.timestamp = rating.getTimestamp();
        this.rating = rating.getRating();

        if (userMap != null) {
            this.deltas = rating.getDeltas().entrySet().stream()
                    .flatMap(entry -> Optional.ofNullable(userMap.get(entry.getKey()))
                            .map(user -> new DeltaView(user, entry.getValue()))
                            .stream())
                    .collect(Collectors.toList());
        } else {
            this.deltas = null;
        }
    }

    @Value
    public static class DeltaView {
        UserView user;
        float delta;

        private DeltaView(User user, float delta) {
            this.user = new UserView(user.getId(), user, null);
            this.delta = delta;
        }
    }
}
