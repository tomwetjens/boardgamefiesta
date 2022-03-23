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

import java.time.Instant;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Log {

    private final NavigableMap<Instant, LogEntry> map = new TreeMap<>(Comparator.reverseOrder());

    public Stream<LogEntry> since(Instant since, int limit) {
        return map.headMap(since).values().stream().limit(limit);
    }

    public Stream<LogEntry> before(Instant before, int limit) {
        return map.tailMap(before).values().stream().limit(limit);
    }

    protected void add(LogEntry logEntry) {
        map.put(logEntry.getTimestamp(), logEntry);
    }

    public Stream<LogEntry> stream() {
        return map.values().stream();
    }

}
