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

import com.boardgamefiesta.api.domain.Action;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Lazy;
import com.boardgamefiesta.domain.table.Log;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableTest {

    public static final Instant T = LocalDateTime.of(2020, 1, 1, 12, 0, 0).toInstant(ZoneOffset.UTC);
    public static final Instant T_MINUS_1 = T.minusMillis(1);
    public static final Instant T_MINUS_2 = T_MINUS_1.minusMillis(1);

    @Mock
    CDI<Object> cdi;

    @Mock
    BeanManager beanManager;

    @BeforeEach
    void setUp() {
        when(cdi.getBeanManager()).thenReturn(beanManager);
        CDI.setCDIProvider(() -> cdi);
    }

    @Nested
    class Undo {

        @Mock
        Game game;

        @Mock
        State currentState, currentMinus1, currentMinus2;

        @Mock
        Action action;

        User.Id userId1 = User.Id.of("userA");
        User.Id userId2 = User.Id.of("userB");

        Player playerA = Player.builder()
                .id(Player.Id.of("playerA"))
                .status(Player.Status.ACCEPTED)
                .created(Instant.now())
                .updated(Instant.now())
                .type(Player.Type.USER)
                .userId(userId1)
                .color(PlayerColor.RED)
                .turn(true)
                .build();

        Player playerB = Player.builder()
                .id(Player.Id.of("playerB"))
                .status(Player.Status.ACCEPTED)
                .created(Instant.now())
                .updated(Instant.now())
                .type(Player.Type.USER)
                .userId(userId2)
                .color(PlayerColor.BLUE)
                .build();

        @Mock
        com.boardgamefiesta.api.domain.Player currentPlayer = new com.boardgamefiesta.api.domain.Player("playerA", PlayerColor.RED, com.boardgamefiesta.api.domain.Player.Type.HUMAN);

        @Test
        void undo() {
            when(currentState.getCurrentPlayers()).thenReturn(Collections.singleton(currentPlayer));
            when(currentState.canUndo()).thenReturn(true);

            when(currentMinus1.getCurrentPlayers()).thenReturn(Collections.singleton(currentPlayer));
            when(currentMinus1.canUndo()).thenReturn(true);
            when(currentMinus1.getPlayerByName("playerA")).thenReturn(Optional.of(currentPlayer));
            when(currentMinus1.getPlayerByName("playerB")).thenReturn(Optional.empty());

            when(currentMinus2.getCurrentPlayers()).thenReturn(Collections.singleton(currentPlayer));
            when(currentMinus2.getPlayerByName("playerA")).thenReturn(Optional.of(currentPlayer));
            when(currentMinus2.getPlayerByName("playerB")).thenReturn(Optional.empty());

            var currentMinus2HistoricState = Table.HistoricState.builder()
                    .state(currentMinus2)
                    .timestamp(T_MINUS_2)
                    .previous(Lazy.of(Optional.empty()))
                    .build();
            var currentMinus1HistoricState = Table.HistoricState.builder()
                    .state(currentMinus1)
                    .timestamp(T_MINUS_1)
                    .previous(Lazy.of(Optional.of(currentMinus2HistoricState)))
                    .build();

            var table = Table.builder()
                    .id(Table.Id.of("tableId"))
                    .type(Table.Type.REALTIME)
                    .mode(Table.Mode.NORMAL)
                    .visibility(Table.Visibility.PRIVATE)
                    .game(game)
                    .options(new Options(Collections.emptyMap()))
                    .created(Instant.now())
                    .updated(Instant.now())
                    .players(new HashSet<>(Set.of(playerA, playerB)))
                    .ownerId(userId1)
                    .status(Table.Status.STARTED)
                    .log(new Log())
                    .currentState(Lazy.of(Optional.of(Table.CurrentState.builder()
                            .state(currentState)
                            .timestamp(T)
                            .previous(Lazy.of(Optional.of(currentMinus1HistoricState)))
                            .changed(false)
                            .build())))
                    .build();

            var beforeUndo = Instant.now();
            table.undo(playerA);

            assertThat(table.getState()).isSameAs(currentMinus1);
            assertThat(table.getCurrentState().get().get().getTimestamp()).isAfterOrEqualTo(beforeUndo);
            assertThat(table.getCurrentState().get().get().isChanged()).isTrue();

            beforeUndo = Instant.now();
            table.undo(playerA);

            assertThat(table.getState()).isSameAs(currentMinus2);
            assertThat(table.getCurrentState().get().get().getTimestamp()).isAfterOrEqualTo(beforeUndo);
            assertThat(table.getCurrentState().get().get().isChanged()).isTrue();

            var beforePerform = Instant.now();
            table.perform(playerA, action);

            assertThat(table.getState()).isSameAs(currentMinus2);
            assertThat(table.getCurrentState().get().get().getTimestamp()).isAfterOrEqualTo(beforePerform);
            assertThat(table.getCurrentState().get().get().isChanged()).isTrue();
        }
    }
}