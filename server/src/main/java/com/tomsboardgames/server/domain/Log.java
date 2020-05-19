package com.tomsboardgames.server.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Log {

    private final NavigableMap<Instant, LogEntry> map = new TreeMap<>(Comparator.reverseOrder());

    public Stream<LogEntry> since(Instant since) {
        return map.headMap(since).values().stream();
    }

    protected void add(LogEntry logEntry) {
        map.put(logEntry.getTimestamp(), logEntry);
    }

    public Stream<LogEntry> stream() {
        return map.values().stream();
    }
}
