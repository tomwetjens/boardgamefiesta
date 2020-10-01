package com.boardgamefiesta.server.domain.rating;

import com.boardgamefiesta.server.domain.Game;
import com.boardgamefiesta.server.domain.Table;
import com.boardgamefiesta.server.domain.User;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Ratings {

    Stream<Rating> findHistoric(User.Id userId, Game.Id gameId, Instant from, Instant to);

    Optional<Rating> findByTable(User.Id userId, Table.Id tableId);

    Rating findLatest(User.Id userId, Game.Id gameId);

    void addAll(Collection<Rating> ratings);

    Stream<User.Id> findRanking(Game.Id gameId, int maxResults);

    Optional<Integer> findRank(User.Id userId, Game.Id gameId);

}
