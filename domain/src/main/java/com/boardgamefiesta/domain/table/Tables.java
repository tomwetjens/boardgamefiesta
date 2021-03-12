package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.User;

import java.util.Optional;
import java.util.stream.Stream;

public interface Tables extends Repository {

    Optional<Table> findById(Table.Id id);

    void add(Table table);

    void update(Table table) throws ConcurrentModificationException;

    Stream<Table> findActive(User.Id userId);

    Stream<Table> findRecent(User.Id userId, int maxResults);

    Stream<Table> findRecent(User.Id userId, Game.Id gameId, int maxResults);

    Stream<Table> findAll();

    Stream<Table> findAll(Game.Id gameId, int maxResults);

    Stream<Table> findAllEndedSortedByEndedAscending();

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
