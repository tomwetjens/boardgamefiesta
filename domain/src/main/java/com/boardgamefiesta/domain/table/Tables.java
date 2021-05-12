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

    Stream<Table> findEnded(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to, boolean ascending);

    Stream<Table> findEnded(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to, boolean ascending, @NonNull Table.Id lastEvaluatedId);

    Stream<LogEntry> findLogEntries(@NonNull Table.Id tableId, @NonNull Instant since, @NonNull Instant before, int limit);

    final class ExceedsMaxActiveGames extends AggregateRoot.InvalidCommandException {
        public ExceedsMaxActiveGames() {
            super("EXCEEDS_MAX_ACTIVE_GAMES");
        }
    }

}
