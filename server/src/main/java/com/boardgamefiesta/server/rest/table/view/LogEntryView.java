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
import com.boardgamefiesta.domain.table.LogEntry;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Value
public class LogEntryView {

    Instant timestamp;
    LogEntry.Type type;
    List<String> parameters;

    PlayerSummaryView player;
    UserSummaryView user;
    UserSummaryView otherUser;

    public LogEntryView(@NonNull LogEntry logEntry, @NonNull Function<User.Id, User> userFunction) {
        this.timestamp = logEntry.getTimestamp();
        this.type = logEntry.getType();
        this.parameters = logEntry.getParameters();

        this.player = new PlayerSummaryView(logEntry.getPlayerId().getId());
        this.user = logEntry.getUserId().map(userId -> new UserSummaryView(userFunction.apply(userId))).orElse(null);

        switch (logEntry.getType()) {
            case INVITE:
            case KICK:
                var otherUserId = User.Id.of(logEntry.getParameters().get(0));
                otherUser = new UserSummaryView(userFunction.apply(otherUserId));
                break;
            default:
                otherUser = null;
        }
    }

    @Value
    public static class UserSummaryView {
        String id;
        String username;

        UserSummaryView(User user) {
            id = user.getId().getId();
            username = user.getUsername();
        }
    }

    @Value
    @AllArgsConstructor
    public static class PlayerSummaryView {
        String id;

        PlayerSummaryView(Player player) {
            id = player.getId().getId();
        }
    }

}
