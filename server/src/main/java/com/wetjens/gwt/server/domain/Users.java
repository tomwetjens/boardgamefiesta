package com.wetjens.gwt.server.domain;

import java.time.Instant;
import java.util.Optional;

public interface Users {

    User findById(User.Id id);

    Optional<User> findOptionallyById(User.Id id);

    void add(User user);

    void update(User user);

    void updateLastSeen(User.Id id, Instant lastSeen);
}
