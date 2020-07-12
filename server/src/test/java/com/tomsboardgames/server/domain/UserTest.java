package com.boardgamefiesta.server.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTest {

    @Mock
    private Users users;

    @BeforeEach
    void setUp() {
        var cdi = mock(CDI.class);
        CDI.setCDIProvider(() -> cdi);

        var usersInstance = mock(Instance.class);
        lenient().when(cdi.select(Users.class)).thenReturn(usersInstance);

        lenient().when(usersInstance.get()).thenReturn(users);
    }

    @Nested
    class ValidateBeforeCreate {

        @Test
        void reservedUsername() {
            assertThatThrownBy(() -> User.validateBeforeCreate("admin", "foo@example.com"))
                    .hasMessage(APIError.USERNAME_FORBIDDEN.name());
        }

        @Test
        void badWord() {
            assertThatThrownBy(() -> User.validateBeforeCreate("shit", "foo@example.com"))
                    .hasMessage(APIError.USERNAME_FORBIDDEN.name());
        }

        @Test
        void emailAlreadyInUse() {
            when(users.findByEmail("foo@example.com")).thenReturn(Optional.of(mock(User.class)));

            assertThatThrownBy(() -> User.validateBeforeCreate("foo", "foo@example.com"))
                    .hasMessage(APIError.EMAIL_ALREADY_IN_USE.name());
        }
    }
}