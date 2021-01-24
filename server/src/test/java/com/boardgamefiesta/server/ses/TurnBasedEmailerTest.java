package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.game.Games;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.user.User;
import com.boardgamefiesta.server.domain.user.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnBasedEmailerTest {

    @Mock
    Users users;

    @Mock
    Games games;

    @Mock
    SesClient sesClient;

    @Mock
    User user;

    TurnBasedEmailer turnBasedEmailer;

    @Captor
    ArgumentCaptor<SendEmailRequest> sendEmailRequestCaptor;

    @BeforeEach
    void setUp() {
        turnBasedEmailer = new TurnBasedEmailer(users, games, new Translations(),
                "https://boardgamefiesta.com", "info@boardgamefiesta.com",
                sesClient);

        when(users.findOptionallyById(any())).thenReturn(Optional.of(user));
    }

    @Test
    void newYork() {
        when(user.getLocale()).thenReturn(Locale.forLanguageTag("en"));
        when(user.getTimeZone()).thenReturn(ZoneId.of("America/New_York"));

        turnBasedEmailer.beginTurn(new Table.BeginTurn(
                Game.Id.of("gwt"),
                Table.Id.of("tableId"),
                Table.Type.TURN_BASED,
                Optional.of(User.Id.of("userId")),
                OffsetDateTime.of(2021, 2, 7, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                OffsetDateTime.of(2021, 1, 24, 14, 0, 0, 0, ZoneOffset.UTC).toInstant()
        ));

        verify(sesClient).sendEmail(sendEmailRequestCaptor.capture());

        var sendEmailRequest = sendEmailRequestCaptor.getValue();

        assertThat(sendEmailRequest.message().subject().data()).isEqualTo("Your turn to play Ranchers Of The Old West started at 1/24/21, 9:00 AM");
    }

    @Test
    void amsterdam() {
        when(user.getLocale()).thenReturn(Locale.forLanguageTag("nl"));
        when(user.getTimeZone()).thenReturn(ZoneId.of("Europe/Amsterdam"));

        turnBasedEmailer.beginTurn(new Table.BeginTurn(
                Game.Id.of("gwt"),
                Table.Id.of("tableId"),
                Table.Type.TURN_BASED,
                Optional.of(User.Id.of("userId")),
                OffsetDateTime.of(2021, 2, 7, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                OffsetDateTime.of(2021, 1, 24, 14, 0, 0, 0, ZoneOffset.UTC).toInstant()
        ));

        verify(sesClient).sendEmail(sendEmailRequestCaptor.capture());

        var sendEmailRequest = sendEmailRequestCaptor.getValue();

        assertThat(sendEmailRequest.message().subject().data()).isEqualTo("Jouw beurt om te spelen bij Ranchers Of The Old West gestart op 24-01-2021 15:00");
    }
}