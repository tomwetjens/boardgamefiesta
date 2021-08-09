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

package com.boardgamefiesta.domain.karma;

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.*;

import java.time.Instant;
import java.util.Optional;

@Builder
public class Karma {

    private static final int INITIAL = 50;
    private static final int MIN = 0;
    private static final int MAX = 100;

    @Getter
    private final User.Id userId;

    @Getter
    private final Instant timestamp;

    @Getter
    private final int karma;

    @Getter
    private final int delta;

    @Getter
    @NonNull
    private final Event event;

    private final Table.Id tableId;

    public static Karma initial(User.Id userId) {
        return Karma.builder()
                .userId(userId)
                .timestamp(Instant.now())
                .karma(INITIAL)
                .delta(0)
                .event(Event.INITIAL)
                .build();
    }

    public Karma finishedGame(Instant timestamp, Table.Id tableId) {
        return add(timestamp, Event.FINISH_GAME)
                .tableId(tableId)
                .build();
    }

    public Karma left(Instant timestamp, Table.Id tableId) {
        return add(timestamp, Event.LEFT)
                .tableId(tableId)
                .build();
    }

    public Karma forcedEndTurn(Instant timestamp, Table.Id tableId) {
        return add(timestamp, Event.FORCE_END_TURN)
                .tableId(tableId)
                .build();
    }

    public Karma kicked(Instant timestamp, Table.Id tableId) {
        return add(timestamp, Event.KICKED)
                .tableId(tableId)
                .build();
    }

    private KarmaBuilder add(Instant timestamp, Event event) {
        var newKarma = Math.min(MAX, Math.max(MIN, karma + event.getDelta()));
        return Karma.builder()
                .userId(userId)
                .timestamp(timestamp)
                .event(event)
                .karma(newKarma)
                .delta(newKarma - karma);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Event {
        INITIAL(0),
        UNKNOWN(0),
        FINISH_GAME(10),
        LEFT(-20),
        KICKED(-25),
        FORCE_END_TURN(-10);

        @Getter
        private final int delta;
    }

    public Optional<Table.Id> getTableId() {
        return Optional.of(tableId);
    }
}
