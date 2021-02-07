package com.boardgamefiesta.server.domain;

import com.boardgamefiesta.api.domain.Action;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.table.Log;
import com.boardgamefiesta.server.domain.table.Player;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.user.User;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableTest {

    public static final Instant T = LocalDateTime.of(2020, 1,1 ,12,0,0).toInstant(ZoneOffset.UTC);
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
        com.boardgamefiesta.api.domain.Player currentPlayer;

        @Test
        void undo() {
            when(currentPlayer.getName()).thenReturn("playerA");

            when(currentState.getCurrentPlayers()).thenReturn(Collections.singleton(currentPlayer));
            when(currentState.canUndo()).thenReturn(true);
            when(currentState.getPlayerByName("playerA")).thenReturn(Optional.of(currentPlayer));

            when(currentMinus1.getCurrentPlayers()).thenReturn(Collections.singleton(currentPlayer));
            when(currentMinus1.canUndo()).thenReturn(true);
            when(currentMinus1.getPlayerByName("playerA")).thenReturn(Optional.of(currentPlayer));

            when(currentMinus2.getCurrentPlayers()).thenReturn(Collections.singleton(currentPlayer));
            when(currentMinus2.getPlayerByName("playerA")).thenReturn(Optional.of(currentPlayer));

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
                    .currentState(Optional.of(Table.CurrentState.of(currentState, T, Optional.of(T_MINUS_1))))
                    .historicStates(Table.HistoricStates.of(
                            Table.HistoricState.of(T, Optional.of(T_MINUS_1), currentState),
                            Table.HistoricState.of(T_MINUS_1, Optional.of(T_MINUS_2), currentMinus1),
                            Table.HistoricState.of(T_MINUS_2, Optional.empty(), currentMinus2)))
                    .build();

            table.undo(playerA);

            assertThat(table.getState()).isSameAs(currentMinus1);
            assertThat(table.getCurrentState().get().getTimestamp()).isSameAs(T_MINUS_1);

            table.undo(playerA);

            assertThat(table.getState()).isSameAs(currentMinus2);
            assertThat(table.getCurrentState().get().getTimestamp()).isSameAs(T_MINUS_2);

            var beforePerform = Instant.now();
            table.perform(playerA, action);

            assertThat(table.getState()).isSameAs(currentMinus2);
            assertThat(table.getCurrentState().get().getTimestamp()).isAfterOrEqualTo(beforePerform);
        }
    }
}