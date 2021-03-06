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

package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.table.Log;
import com.boardgamefiesta.domain.table.LogEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Log of a game that is lazily loaded, and tracks which log entries were added,
 * so having only to persist new log entries when saving a game.
 */
class LazyLog extends Log {

    public static final Instant MIN = Instant.ofEpochSecond(0);
    public static final Instant MAX = Instant.ofEpochSecond(253402297199L);

    private final Loader loader;
    private final List<LogEntry> pending = new ArrayList<>();

    LazyLog(Loader loader) {
        this.loader = loader;
    }

    @Override
    public Stream<LogEntry> since(Instant since, int limit) {
        load(since, MAX, limit);
        return super.since(since, limit);
    }

    private void load(Instant from, Instant to, int limit) {
        loader.load(from, to, limit).forEach(super::add);
    }

    @Override
    public Stream<LogEntry> before(Instant before, int limit) {
        load(MIN, before, limit);
        return super.before(before, limit);
    }

    @Override
    public Stream<LogEntry> stream() {
        load(MIN, MAX, Integer.MAX_VALUE);
        return super.stream();
    }

    @Override
    protected void add(LogEntry logEntry) {
        super.add(logEntry);

        // Remember element in separate list, to be able to find newly added elements later
        pending.add(logEntry);
    }

    Stream<LogEntry> pending() {
        return pending.stream().onClose(pending::clear);
    }

    @FunctionalInterface
    public interface Loader {
        /**
         *
         * @param from exclusive
         * @param to exclusive
         * @param limit
         * @return
         */
        Stream<LogEntry> load(Instant from, Instant to, int limit);
    }
}
