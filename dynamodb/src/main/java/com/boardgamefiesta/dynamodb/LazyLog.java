package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.table.Log;
import com.boardgamefiesta.domain.table.LogEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Log of a game that is lazily loaded, and tracks which log entries were added,
 * so having only to persist new log entries when saving a game.
 */
class LazyLog extends Log {

    private final Function<Instant, Stream<LogEntry>> loader;
    private final List<LogEntry> pending = new ArrayList<>();
    private Instant oldestLoaded;

    LazyLog(Function<Instant, Stream<LogEntry>> loader) {
        this.loader = loader;
    }

    @Override
    public Stream<LogEntry> since(Instant since) {
        if (oldestLoaded == null || oldestLoaded.isAfter(since)) {
            loader.apply(since).forEach(super::add);
            oldestLoaded = since;
        }
        return super.since(since);
    }

    @Override
    public Stream<LogEntry> stream() {
        return super.since(Instant.MIN);
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

}
