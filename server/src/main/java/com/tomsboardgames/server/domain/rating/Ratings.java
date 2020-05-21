package com.tomsboardgames.server.domain.rating;

import com.tomsboardgames.server.domain.Table;
import com.tomsboardgames.server.domain.User;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Ratings {

    Stream<Rating> findHistoric(User.Id userId, String gameId, Instant from, Instant to);

    Optional<Rating> findByTable(User.Id userId, Table table);

    Rating findLatest(User.Id userId, String gameId);

    void addAll(Collection<Rating> ratings);

}
