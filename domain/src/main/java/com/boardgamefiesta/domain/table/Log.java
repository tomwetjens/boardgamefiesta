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
