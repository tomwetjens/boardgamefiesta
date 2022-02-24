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

package com.boardgamefiesta.domain.event;

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Builder
public class WebSocketConnection {

    @Getter
    @NonNull
    String id;

    @Getter
    @NonNull
    User.Id userId;

    Table.Id tableId;

    @Getter
    @NonNull
    Status status;

    @Getter
    @NonNull
    Instant created;

    @Getter
    @NonNull
    Instant updated;

    public static WebSocketConnection createForUser(String id, User.Id userId) {
        return WebSocketConnection.builder()
                .id(id)
                .userId(userId)
                .status(Status.INACTIVE)
                .created(Instant.now())
                .updated(Instant.now())
                .build();
    }

    public static WebSocketConnection createForTable(String id, User.Id userId, Table.Id tableId) {
        return WebSocketConnection.builder()
                .id(id)
                .userId(userId)
                .tableId(tableId)
                .status(Status.ACTIVE)
                .created(Instant.now())
                .updated(Instant.now())
                .build();
    }

    public Optional<Table.Id> getTableId() {
        return Optional.ofNullable(tableId);
    }

    public Instant getExpires() {
        return calculateExpires(updated);
    }

    public static Instant calculateExpires(Instant updated) {
        return updated.plus(2, ChronoUnit.MINUTES);
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
