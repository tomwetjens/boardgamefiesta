package com.boardgamefiesta.domain.user;

import com.boardgamefiesta.domain.Repository;

import java.util.Optional;
import java.util.stream.Stream;

public interface Friends extends Repository {

    Optional<Friend> findById(Friend.Id id);

    Stream<Friend> findByUserId(User.Id userId, int maxResults);

    void add(Friend friend);

    void update(Friend friend);

}
