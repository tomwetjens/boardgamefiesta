package com.boardgamefiesta.server.domain.rating;

import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.user.User;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Ratings {

    Stream<Rating> findHistoric(User.Id userId, Game.Id gameId, Instant from, Instant to);

    Optional<Rating> findByTable(User.Id userId, Table.Id tableId);

    Rating findLatest(User.Id userId, Game.Id gameId, Instant before);

    void addAll(Collection<Rating> ratings);

    Stream<Ranking> findRanking(Game.Id gameId, int maxResults);

    Stream<Rating> findAll();

    void delete(Rating rating);
}
