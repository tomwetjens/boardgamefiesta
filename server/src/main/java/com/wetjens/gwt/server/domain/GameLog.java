package com.wetjens.gwt.server.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface GameLog {

    Stream<GameLogEntry> findSince(Game.Id gameId, Instant since);

    void addAll(Collection<GameLogEntry> entries);
}
