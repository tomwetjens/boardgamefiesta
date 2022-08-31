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

package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.api.domain.InGameEvent;
import com.boardgamefiesta.domain.user.User;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LogEntry {

    private static final Duration DEFAULT_RETENTION = Duration.of(365, ChronoUnit.DAYS);

    private static final ThreadLocal<Instant> THREAD_LAST_TIMESTAMP = new ThreadLocal<>();

    @NonNull
    Player.Id playerId;

    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    Type type;

    @NonNull
    List<String> parameters;

    LogEntry(@NonNull Table table, @NonNull InGameEvent event) {
        this.playerId = Player.Id.of(event.getPlayer().getName());
        var player = table.getPlayerById(this.playerId);
        this.userId = player.flatMap(Player::getUserId).orElse(null);
        this.timestamp = generateTimestamp();
        this.type = Type.IN_GAME_EVENT;
        this.parameters = Stream.concat(Stream.of(event.getType()), event.getParameters().stream())
                .collect(Collectors.toList());
    }


    LogEntry(@NonNull Player player, @NonNull Type type) {
        this(player, type, Collections.emptyList());
    }

    LogEntry(@NonNull Player player, @NonNull Type type, @NonNull List<Object> parameters) {
        this.playerId = player.getId();
        this.userId = player.getUserId().orElse(null);
        this.timestamp = generateTimestamp();
        this.type = type;
        this.parameters = parameters.stream().map(Object::toString).collect(Collectors.toList());
    }

    public Optional<User.Id> getUserId() {
        return Optional.ofNullable(userId);
    }

    private static Instant generateTimestamp() {
        // Timestamps are used as key, and must be unique, even when multiple log entries are created at once
        final var now = Instant.now();

        var lastTimestamp = THREAD_LAST_TIMESTAMP.get();

        Instant timestamp;
        if (lastTimestamp == null || lastTimestamp.toEpochMilli() < now.toEpochMilli()) {
            timestamp = now;
        } else {
            timestamp = lastTimestamp.plusMillis(1);
        }

        THREAD_LAST_TIMESTAMP.set(timestamp);

        return timestamp;
    }

    public enum Type {
        ACCEPT,
        REJECT,
        START,
        INVITE,
        CREATE,
        KICK,
        LEFT,
        UNDO,
        JOIN,
        IN_GAME_EVENT,
        BEGIN_TURN,
        BEGIN_TURN_NR,
        END_TURN,
        END_TURN_NR,
        SKIP,
        END,
        FORCE_END_TURN
    }
}
