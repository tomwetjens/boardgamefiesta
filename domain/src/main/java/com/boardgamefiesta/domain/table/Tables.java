package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.User;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface Tables extends Repository {

    int MAX_ACTIVE_GAMES = 10;
    int MAX_ACTIVE_REALTIME_GAMES = MAX_ACTIVE_GAMES;

    Optional<Table> findById(Table.Id id);

    void add(Table table);

    void update(Table table) throws ConcurrentModificationException;

    Stream<Table> findActive(User.Id userId);

    /**
     * @param userId
     * @param maxResults min 1, max 100
     * @return
     */
    Stream<Table> findAll(User.Id userId, int maxResults);

    /**
     * @param userId
     * @param gameId
     * @param maxResults min 1, max 100
     * @return
     */
    Stream<Table> findAll(User.Id userId, Game.Id gameId, int maxResults);

    /**
     * @param gameId
     * @param maxResults min 1, max 100
     * @return
     */
    Stream<Table> findEnded(Game.Id gameId, int maxResults);

    /**
     * @param gameId
     * @param maxResults min 1, max 100
     * @param from       table must have an ended timestamp greater than or equal to this value
     * @return
     */
    Stream<Table> findEnded(Game.Id gameId, int maxResults, Instant from);

    final class ExceedsMaxRealtimeGames extends AggregateRoot.InvalidCommandException {
        public ExceedsMaxRealtimeGames() {
            super("EXCEEDS_MAX_REALTIME_GAMES");
        }
    }

    final class ExceedsMaxActiveGames extends AggregateRoot.InvalidCommandException {
        public ExceedsMaxActiveGames() {
            super("EXCEEDS_MAX_ACTIVE_GAMES");
        }
    }

}
