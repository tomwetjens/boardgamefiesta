package com.boardgamefiesta.domain.user;

import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.Repository;

import java.util.Optional;
import java.util.stream.Stream;

public interface Users extends Repository {

    User findById(User.Id id, boolean consistentRead) throws NotFoundException;

    Optional<User> findOptionallyById(User.Id id);

    Stream<User> findByUsernameStartsWith(String username);

    Optional<User> findByEmail(String email);

    void add(User user);

    void update(User user) throws ConcurrentModificationException;

    void validateBeforeAdd(String username, String email);

    final class EmailAlreadyInUse extends DuplicateException {
        public EmailAlreadyInUse() {
            super("EMAIL_ALREADY_IN_USE");
        }
    }

}
