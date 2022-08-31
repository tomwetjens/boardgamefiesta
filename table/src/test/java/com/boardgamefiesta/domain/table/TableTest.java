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

package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.api.domain.*;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
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

            when(currentMinus2.getCurrentPlayers()).thenReturn(Collections.singleton(currentPlayer));

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

    @Nested
    class Fork {

        @Mock
        GameProvider<State> provider;

        Game game;

        User owningUser = User.createAutomatically("owner", "owner@example.com");
        User otherUser = User.createAutomatically("otherUser", "otherUser@example.com");
        User yetAnotherUser = User.createAutomatically("yetAnotherUser", "yetAnotherUser@example.com");
        User forkingUser = User.createAutomatically("forkingUser", "forkingUser@example.com");
        User invitedUser = User.createAutomatically("invitedUser", "invitedUser@example.com");


        @Mock
        State initialState;

        @BeforeEach
        void setUp() {
            Table.RANDOM = new Random(0); // Make test deterministic

            game = Game.create(Game.Id.fromString("test"), provider);

            lenient().when(provider.hasAutoma()).thenReturn(true);
            lenient().when(provider.getMinNumberOfPlayers()).thenReturn(2);
            lenient().when(provider.getMaxNumberOfPlayers()).thenReturn(4);
            lenient().when(provider.getSupportedColors()).thenReturn(Set.of(PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.GREEN, PlayerColor.RED));
            lenient().when(provider.start(anySet(), any(Options.class), any(InGameEventListener.class), any(Random.class))).thenReturn(initialState);
        }

        @Test
        void notStarted() {
            var table = Table.create(game, Table.Type.REALTIME, Table.Mode.NORMAL, owningUser, new Options(new HashMap<>()));

            assertThatThrownBy(() -> table.fork(Instant.now(), Table.Type.REALTIME, Table.Mode.NORMAL, owningUser))
                    .isInstanceOf(Table.HistoryNotAvailable.class);
        }

        @Test
        void fork() {
            var table = Table.create(game, Table.Type.REALTIME, Table.Mode.NORMAL, owningUser, new Options(new HashMap<>(Map.of("aOption", "aValue"))));

            var owningPlayer = table.getPlayers().iterator().next();
            table.makePublic();
            var otherPlayer = table.join(otherUser);
            var yetAnotherPlayer = table.join(yetAnotherUser);

            when(initialState.getCurrentPlayers()).thenReturn(Set.of(owningPlayer.asPlayer()));
            table.start();

            table.perform(owningPlayer, new Action() {
            });

            table.perform(owningPlayer, new Action() {
            });

            table.perform(owningPlayer, new Action() {
            });
            var state = table.getCurrentState().get().orElseThrow().getState();

            var beforeFork = Instant.now();
            var forked = table.fork(Instant.now(), Table.Type.REALTIME, Table.Mode.NORMAL, forkingUser);

            assertThat(forked.getId()).isNotEqualTo(table.getId());
            assertThat(forked.getGame()).isSameAs(table.getGame());
            assertThat(forked.getStatus()).isEqualTo(Table.Status.NEW);
            assertThat(forked.getCreated()).isAfterOrEqualTo(beforeFork);
            assertThat(forked.getUpdated()).isAfterOrEqualTo(beforeFork);
            assertThat(forked.getMinNumberOfPlayers()).isEqualTo(3);
            assertThat(forked.getMaxNumberOfPlayers()).isEqualTo(3);
            assertThat(forked.getOptions()).isEqualTo(table.getOptions());
            assertThat(forked.getOwnerId()).isEqualTo(forkingUser.getId());
            assertThat(forked.getCurrentState().get().orElseThrow().getState()).isSameAs(state);
            assertThat(forked.getCurrentState().get().orElseThrow().getTimestamp()).isAfterOrEqualTo(beforeFork);
            assertThat(forked.getCurrentState().get().orElseThrow().getPrevious().get()).isEmpty();
            assertThat(forked.getCurrentState().get().orElseThrow().isChanged()).isTrue();

            assertThat(forked.getPlayers()).hasSize(1);
            var forkingPlayer = forked.getPlayers().iterator().next();
            assertThat(forkingPlayer.getUserId()).contains(forkingUser.getId());
            assertThat(forkingPlayer.getStatus()).isEqualTo(Player.Status.ACCEPTED);
            assertThat(forkingPlayer.getColor()).isNotEmpty();
            assertThat(forkingPlayer.getCreated()).isAfterOrEqualTo(beforeFork);
            assertThat(forkingPlayer.getUpdated()).isAfterOrEqualTo(beforeFork);

            // All original players should be created as seats in the fork
            assertThat(forked.getSeats()).hasSize(3);
            assertThat(forked.getSeats()).extracting(Seat::getPlayerId).containsExactlyInAnyOrder(
                    owningPlayer.getId(),
                    otherPlayer.getId(),
                    yetAnotherPlayer.getId());
            assertThat(forked.getSeats()).extracting(Seat::getPlayerColor).containsExactlyInAnyOrder(
                    owningPlayer.getColor(),
                    otherPlayer.getColor(),
                    yetAnotherPlayer.getColor());

            var invitedPlayer = forked.invite(invitedUser);
            assertThat(forked.getPlayers()).containsExactlyInAnyOrder(forkingPlayer, invitedPlayer);
            assertThat(invitedPlayer.getUserId()).contains(invitedUser.getId());
            assertThat(invitedPlayer.getStatus()).isEqualTo(Player.Status.INVITED);

            var computerPlayer = forked.addComputer();
            assertThat(forked.getPlayers()).containsExactlyInAnyOrder(forkingPlayer, invitedPlayer, computerPlayer);
            assertThat(computerPlayer.getUserId()).isEmpty();
            assertThat(computerPlayer.getStatus()).isEqualTo(Player.Status.ACCEPTED);

            // All player ids from the original game should be mapped
            assertThat(forked.getPlayers()).extracting(Player::getId).containsExactlyInAnyOrder(
                    owningPlayer.getId(),
                    otherPlayer.getId(),
                    yetAnotherPlayer.getId());
            assertThat(forked.getPlayers()).extracting(Player::getColor).containsExactlyInAnyOrder(
                    owningPlayer.getColor(),
                    otherPlayer.getColor(),
                    yetAnotherPlayer.getColor());

            // TODO More assertions
        }

        // TODO After fork(), join() should assign unfilled seat (if available, else error)
    }
}