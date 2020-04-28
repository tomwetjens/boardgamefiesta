package com.wetjens.gwt.server.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Stream;

public interface LogEntries {

    Stream<LogEntry> findSince(Game.Id gameId, Instant since);

    void addAll(Collection<LogEntry> entries);

    void add(LogEntry entry);

}
