package com.boardgamefiesta.domain.user;

import com.boardgamefiesta.domain.Repository;

import java.util.Optional;
import java.util.stream.Stream;

public interface Users extends Repository {

    User findById(User.Id id, boolean consistentRead) throws NotFoundException;

    Optional<User> findOptionallyById(User.Id id);

    Stream<User> findByUsernameStartsWith(String username, int maxResults);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByCognitoUsername(String cognitoUsername);

    void add(User user);

    void update(User user) throws ConcurrentModificationException;

    void validateBeforeAdd(String email);

    final class EmailAlreadyInUse extends DuplicateException {
        public EmailAlreadyInUse() {
            super("EMAIL_ALREADY_IN_USE", "E-mail address already registered");
        }
    }

}
