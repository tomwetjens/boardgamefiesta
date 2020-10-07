package com.boardgamefiesta.server.domain.user;

import com.boardgamefiesta.server.domain.DomainService;

import java.util.Optional;
import java.util.stream.Stream;

public interface Users extends DomainService {

    static Users instance() {
        return DomainService.instance(Users.class);
    }

    User findById(User.Id id, boolean consistentRead);

    Optional<User> findOptionallyById(User.Id id);

    Stream<User> findByUsernameStartsWith(String username);

    Optional<User> findByEmail(String email);

    void add(User user);

    void update(User user) throws UserConcurrentlyModifiedException;

    void updateLastSeen(User user) throws UserConcurrentlyModifiedException;

    class UserConcurrentlyModifiedException extends Exception {
        public UserConcurrentlyModifiedException(Throwable cause) {
            super(cause);
        }
    }
}
