package com.boardgamefiesta.domain;

import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UserTest {

    @Nested
    class ValidateBeforeCreate {

        @Test
        void reservedUsername() {
            assertThatThrownBy(() -> User.validateBeforeCreate("admin"))
                    .hasMessage("USERNAME_FORBIDDEN");
        }

        @Test
        void badWord() {
            assertThatThrownBy(() -> User.validateBeforeCreate("shit"))
                    .hasMessage("USERNAME_FORBIDDEN");
        }

    }
}