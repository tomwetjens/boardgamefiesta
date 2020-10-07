package com.boardgamefiesta.server.domain.user;

import javax.ws.rs.NotFoundException;
import java.util.stream.Stream;

public interface Friends {

    Friend findById(Friend.Id id);

    Stream<Friend> findByUserId(User.Id userId, int maxResults);

    void add(Friend friend);

    void update(Friend friend);

    class FriendNotFoundException extends NotFoundException {
    }
}
