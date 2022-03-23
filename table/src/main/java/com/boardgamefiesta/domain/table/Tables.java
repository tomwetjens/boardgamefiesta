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

import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface Tables extends Repository {

    int MAX_ACTIVE_GAMES = 50;

    Instant MIN_TIMESTAMP = Instant.ofEpochSecond(0);
    Instant MAX_TIMESTAMP = Instant.parse("9999-12-31T23:59:59.999Z");

    Optional<Table> findById(Table.Id id);

    void add(Table table);

    void update(Table table) throws ConcurrentModificationException;

    Stream<Table> findActive(User.Id userId);

    Stream<Table> findAll(@NonNull User.Id userId, int maxResults);

    Stream<Table> findAll(@NonNull User.Id userId, @NonNull Game.Id gameId, int maxResults);

    Stream<Table> findStarted(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to);

    Stream<Table> findStarted(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to, @NonNull Table.Id lastEvaluatedId);

    Stream<Table> findOpen(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to);

    Stream<Table> findOpen(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to, @NonNull Table.Id lastEvaluatedId);

    Stream<Table> findEndedWithHumanPlayers(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to, boolean ascending);

    Stream<Table> findEndedWithHumanPlayers(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to, boolean ascending, @NonNull Table.Id lastEvaluatedId);

    Stream<LogEntry> findLogEntries(@NonNull Table.Id tableId, @NonNull Instant since, @NonNull Instant before, int limit);

    final class ExceedsMaxActiveGames extends AggregateRoot.InvalidCommandException {
        public ExceedsMaxActiveGames() {
            super("EXCEEDS_MAX_ACTIVE_GAMES");
        }
    }

}
