package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTemplatesTest {

    static final User.Id USER_ID = User.Id.of("A");
    static final Game.Id GAME_ID = Game.Id.of("gwt");
    static final Table.Id TABLE_ID = Table.Id.of("tableId");

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
                GAME_ID,
                TABLE_ID,
                Table.Type.TURN_BASED,
                Optional.of(USER_ID),
                OffsetDateTime.of(2021, 2, 7, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                OffsetDateTime.of(2021, 1, 24, 14, 0, 0, 0, ZoneOffset.UTC).toInstant()
        ), user);

        assertThat(message.subject().data()).startsWith("Jouw beurt om te spelen bij Ranchers Of The Old West gestart op 24-01-2");
    }

    @Test
    void ended() {
        when(user.getLocale()).thenReturn(Locale.forLanguageTag("en-US"));
        when(user.getTimeZone()).thenReturn(ZoneId.of("America/New_York"));

        var game = mock(Game.class);
        when(game.getId()).thenReturn(GAME_ID);

        var player = mock(Player.class);
        when(player.getUserId()).thenReturn(Optional.of(USER_ID));

        var table = mock(Table.class);
        when(table.getId()).thenReturn(TABLE_ID);
        when(table.getGame()).thenReturn(game);
        when(table.getEnded()).thenReturn(Instant.parse("2021-03-23T16:46:00.000Z"));

        var userMap = Map.of(USER_ID, user);

        var message = emailTemplates.createEndedMessage(table, player, userMap);

        assertThat(message.subject().data()).isEqualTo("Ranchers Of The Old West has ended at 3/23/21, 12:46 PM");
        assertThat(message.body().html().data()).isEqualTo("<p>Howdy!</p><br/>" +
                "<p>Your game of Ranchers Of The Old West has ended at 3/23/21, 12:46 PM.</p>" +
                "<p><a href=\"https://boardgamefiesta.com/gwt/tableId\">Go to table</a></p><br/>" +
                "Sincerely,<br/>Board Game Fiesta<br/>" +
                "<a href=\"https://boardgamefiesta.com\">https://boardgamefiesta.com</a>");
    }
}