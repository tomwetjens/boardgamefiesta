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

package com.boardgamefiesta.domain;

import com.boardgamefiesta.domain.exception.DomainException;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class UserTest {

    @Nested
    class ValidateBeforeCreate {

        @Test
        void reservedUsername() {
            assertThatThrownBy(() -> User.validateUsername("admin"))
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode()).isEqualTo("USERNAME_FORBIDDEN"));
        }

        @Test
        void badWord() {
            assertThatThrownBy(() -> User.validateUsername("shit"))
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode()).isEqualTo("USERNAME_FORBIDDEN"));
        }

    }

    @Nested
    class IdTest {
        @Test
        void check() {
            assertThat(User.Id.check(User.Id.generate().getId())).isTrue();
        }
        @Test
        void check2() {
            assertThat(User.Id.check("abc-def")).isFalse();
        }
    }
}