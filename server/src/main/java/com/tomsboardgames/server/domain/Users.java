package com.tomsboardgames.server.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface Users extends DomainService {

    static Users instance() {
        return DomainService.instance(Users.class);
    }

    User findById(User.Id id);

    Optional<User> findOptionallyById(User.Id id);

    Stream<User> findByUsernameStartsWith(String username);

    Optional<User> findByEmail(String email);

    void add(User user);

    void update(User user);

    void updateLastSeen(User.Id id, Instant lastSeen);

}
