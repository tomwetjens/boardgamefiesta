/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.domain.email;

import com.boardgamefiesta.domain.email.velocity.VelocityEmailTemplates;
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
    static final User.Id OTHER_USER_ID = User.Id.of("B");
    static final Game.Id GAME_ID = Game.Id.of("gwt");
    static final Table.Id TABLE_ID = Table.Id.of("tableId");

    @Mock
    User user;

    EmailTemplates emailTemplates;

    @BeforeEach
    void setUp() {
        emailTemplates = new VelocityEmailTemplates(new Translations(), "https://boardgamefiesta.com");
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

        assertThat(message.getSubject()).isEqualTo("Your turn to play Ranchers Of The Old West started at 1/24/21, 9:00 AM");
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

        assertThat(message.getSubject()).startsWith("Jouw beurt om te spelen bij Ranchers Of The Old West gestart op 24-01-2");
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

        assertThat(message.getSubject()).isEqualTo("Ranchers Of The Old West has ended at 3/23/21, 12:46 PM");
    }

    @Test
    void endedMissingTranslations() {
        when(user.getLocale()).thenReturn(Locale.forLanguageTag("it-IT"));
        when(user.getTimeZone()).thenReturn(ZoneId.of("Europe/Rome"));

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

        assertThat(message.getSubject()).isEqualTo("Ranchers Of The Old West has ended at 23/03/21, 17:46");
    }

    @Test
    void invited() {
        when(user.getLocale()).thenReturn(Locale.forLanguageTag("en-US"));
        when(user.getTimeZone()).thenReturn(ZoneId.of("America/New_York"));

        var host = mock(User.class);
        when(host.getUsername()).thenReturn("wetgos");

        var message = emailTemplates.createInvitedMessage(new Table.Invited(TABLE_ID, Table.Type.TURN_BASED, USER_ID, GAME_ID, OTHER_USER_ID), user, host);

        assertThat(message.getSubject()).isEqualTo("You're invited to play Ranchers Of The Old West with wetgos");
    }
}