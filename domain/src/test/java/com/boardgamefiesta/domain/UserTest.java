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