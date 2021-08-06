/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.user;

import com.boardgamefiesta.domain.Repository;

import java.util.Optional;
import java.util.stream.Stream;

public interface Users extends Repository {

    Optional<User> findById(User.Id id);

    Stream<User> findByUsernameStartsWith(String username, int maxResults);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User.Id> findIdByCognitoUsername(String cognitoUsername);

    void add(User user);

    void update(User user) throws ConcurrentModificationException;

    void validateBeforeAdd(String email);

    Stream<User> findByIds(Stream<User.Id> ids);

    final class EmailAlreadyInUse extends DuplicateException {
        public EmailAlreadyInUse() {
            super("EMAIL_ALREADY_IN_USE", "E-mail address already registered");
        }
    }

}
