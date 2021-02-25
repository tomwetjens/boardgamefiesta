package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTemplatesTest {

    @Mock
    User user;

    EmailTemplates emailTemplates;

    @BeforeEach
    void setUp() {
        emailTemplates = new EmailTemplates(new Translations(), "https://boardgamefiesta.com");
    }

    @Test
    void newYork() {
        when(user.getLocale()).thenReturn(Locale.forLanguageTag("en-US"));
        when(user.getTimeZone()).thenReturn(ZoneId.of("America/New_York"));

        var message = emailTemplates.createBeginTurnMessage(new Table.BeginTurn(
                Game.Id.of("gwt"),
                Table.Id.of("tableId"),
                Table.Type.TURN_BASED,
                Optional.of(User.Id.of("userId")),
                OffsetDateTime.of(2021, 2, 7, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                OffsetDateTime.of(2021, 1, 24, 14, 0, 0, 0, ZoneOffset.UTC).toInstant()
        ), user);

        assertThat(message.subject().data()).isEqualTo("Your turn to play Ranchers Of The Old West started at 1/24/21, 9:00 AM");
    }

    @Test
    void amsterdam() {
        when(user.getLocale()).thenReturn(Locale.forLanguageTag("nl-NL"));
        when(user.getTimeZone()).thenReturn(ZoneId.of("Europe/Amsterdam"));

        var message = emailTemplates.createBeginTurnMessage(new Table.BeginTurn(
                Game.Id.of("gwt"),
                Table.Id.of("tableId"),
                Table.Type.TURN_BASED,
                Optional.of(User.Id.of("userId")),
                OffsetDateTime.of(2021, 2, 7, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                OffsetDateTime.of(2021, 1, 24, 14, 0, 0, 0, ZoneOffset.UTC).toInstant()
        ), user);

        assertThat(message.subject().data()).startsWith("Jouw beurt om te spelen bij Ranchers Of The Old West gestart op 24-01-2");
    }
}