package com.boardgamefiesta.domain.rating;

import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Ratings extends Repository {

    Stream<Rating> findHistoric(User.Id userId, Game.Id gameId, Instant from, Instant to);

    Optional<Rating> findByTable(User.Id userId, Table.Id tableId);

    Rating findLatest(User.Id userId, Game.Id gameId, Instant before);

    void addAll(Collection<Rating> ratings);

    Stream<Ranking> findRanking(Game.Id gameId, int maxResults);

}
