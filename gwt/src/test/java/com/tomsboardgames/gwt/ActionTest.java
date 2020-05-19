package com.tomsboardgames.gwt;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionTest {

    @ParameterizedTest
    @MethodSource("allActions")
    void accessible(Class<? extends Action> action) {
        assertThat(action).isPublic();
        assertThat(action.getConstructors()).isNotEmpty();
        assertThat(Modifier.isAbstract(action.getModifiers())).isFalse();
    }

    static Stream<Arguments> allActions() {
        return Arrays.stream(Action.class.getDeclaredClasses())
                .filter(Action.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .map(Arguments::of);
    }

    Player currentPlayer = new Player("Player A", PlayerColor.RED);
    Player otherPlayer = new Player("Player B", PlayerColor.BLUE);

    @Mock
    Game game;

    @Mock
    PlayerState currentPlayerState;

    @Mock
    PlayerState otherPlayerState;

    @Mock
    RailroadTrack railroadTrack;

    @Mock
    Trail trail;

    @BeforeEach
    void setUp() {
        lenient().when(game.getCurrentPlayer()).thenReturn(currentPlayer);
        lenient().when(game.currentPlayerState()).thenReturn(currentPlayerState);
        lenient().when(game.getRailroadTrack()).thenReturn(railroadTrack);
        lenient().when(game.getTrail()).thenReturn(trail);
        lenient().when(game.getPlayers()).thenReturn(List.of(currentPlayer, otherPlayer));
        lenient().when(game.playerState(currentPlayer)).thenReturn(currentPlayerState);
        lenient().when(game.playerState(otherPlayer)).thenReturn(otherPlayerState);
    }

    @Nested
    class DeliverToCity {

        @Test
        void notEnoughCertificates() {
            // TODO
        }

        @Test
        void passedNotAllSignals() {
            // TODO
        }

        @Test
        void passedAllSignals() {
            when(currentPlayerState.handValue()).thenReturn(11);
            when(railroadTrack.signalsPassed(currentPlayer)).thenReturn(7);

            Action.DeliverToCity deliverToCity = new Action.DeliverToCity(City.EL_PASO, 1);
            deliverToCity.perform(game, new Random(0));

            verify(currentPlayerState).gainDollars(12);
            verify(currentPlayerState).discardHand();
            verify(trail).moveToStart(currentPlayer);
        }

        @Test
        void spendOnlyPermCerts() {
            // TODO
        }

        @Test
        void spendTempCerts() {
            // TODO
        }

        @Test
        void toEventParams() {
            when(currentPlayerState.handValue()).thenReturn(11);
            when(railroadTrack.signalsPassed(currentPlayer)).thenReturn(4);

            Action.DeliverToCity deliverToCity = new Action.DeliverToCity(City.EL_PASO, 1);
            List<String> eventParams = deliverToCity.toEventParams(game);

            assertThat(eventParams).containsExactly("EL_PASO", "1", "9");
        }
    }

    @Nested
    class Move {

        @Test
        void move() {
            Location b = mock(Location.class);
            when(b.getHand()).thenReturn(Hand.NONE);

            Location a = mock(Location.class);
            when(a.isDirect(b)).thenReturn(true);

            when(currentPlayerState.getStepLimit(anyInt())).thenReturn(3);

            when(trail.getCurrentLocation(currentPlayer)).thenReturn(Optional.of(a));

            Action.Move move = new Action.Move(List.of(b));
            move.perform(game, new Random(0));

            verify(trail).movePlayer(currentPlayer, b);
        }

        @Test
        void atLeastSteps() {
            Action.Move move = new Action.Move(Collections.emptyList());
            assertThatThrownBy(() -> move.perform(game, new Random(0)))
                    .hasMessage(GWTError.MUST_MOVE_AT_LEAST_STEPS.name());

            verifyNoMoreInteractions(ignoreStubs(trail));
        }

        @Test
        void exceedStepLimit() {
            Location d = mock(Location.class);
            Location c = mock(Location.class);
            Location b = mock(Location.class);
            Location a = mock(Location.class);

            when(currentPlayerState.getStepLimit(anyInt())).thenReturn(3);

            Action.Move move = new Action.Move(List.of(a, b, c, d));
            assertThatThrownBy(() -> move.perform(game, new Random(0)))
                    .hasMessage(GWTError.STEPS_EXCEED_LIMIT.name());

            verifyNoMoreInteractions(ignoreStubs(trail));
        }

        @Test
        void notDirectSteps() {
            Location b = mock(Location.class);

            Location a = mock(Location.class);
            when(a.isDirect(b)).thenReturn(false);

            when(currentPlayerState.getStepLimit(anyInt())).thenReturn(3);

            when(trail.getCurrentLocation(currentPlayer)).thenReturn(Optional.of(a));

            Action.Move move = new Action.Move(List.of(b));
            assertThatThrownBy(() -> move.perform(game, new Random(0)))
                    .hasMessage(GWTError.CANNOT_STEP_DIRECTLY_FROM_TO.name());

            verifyNoMoreInteractions(ignoreStubs(trail));
        }


        @Test
        void notPayFeeToSelf() {
            Location b = mock(Location.class);
            when(b.getHand()).thenReturn(Hand.NONE);

            PlayerBuilding playerBuilding = mock(PlayerBuilding.class);
            when(playerBuilding.getPlayer()).thenReturn(currentPlayer);

            Location.BuildingLocation a1 = mock(Location.BuildingLocation.class);
            when(a1.getHand()).thenReturn(Hand.GREEN);
            when(a1.isDirect(b)).thenReturn(true);
            when(a1.getBuilding()).thenReturn(Optional.of(playerBuilding));

            Location a = mock(Location.class);
            when(a.isDirect(a1)).thenReturn(true);

            when(currentPlayerState.getStepLimit(anyInt())).thenReturn(3);
            when(currentPlayerState.getBalance()).thenReturn(10);

            when(trail.getCurrentLocation(currentPlayer)).thenReturn(Optional.of(a));

            Action.Move move = new Action.Move(List.of(a1, b));
            move.perform(game, new Random(0));

            verify(trail).movePlayer(currentPlayer, b);
            verifyNoMoreInteractions(ignoreStubs(currentPlayerState));
        }

        @Test
        void payFeeToBank() {
            Location b = mock(Location.class);
            when(b.getHand()).thenReturn(Hand.NONE);

            Location flood1 = mock(Location.class);
            when(flood1.getHand()).thenReturn(Hand.GREEN);
            when(flood1.isDirect(b)).thenReturn(true);

            Location a = mock(Location.class);
            when(a.isDirect(flood1)).thenReturn(true);

            when(currentPlayerState.getStepLimit(anyInt())).thenReturn(3);
            when(currentPlayerState.getBalance()).thenReturn(10);

            when(trail.getCurrentLocation(currentPlayer)).thenReturn(Optional.of(a));

            Action.Move move = new Action.Move(List.of(flood1, b));
            move.perform(game, new Random(0));

            verify(currentPlayerState).payDollars(2);
            verify(trail).movePlayer(currentPlayer, b);
            verifyNoInteractions(otherPlayerState);
        }

        @Test
        void payFeeToOtherPlayer() {
            Location b = mock(Location.class);
            when(b.getHand()).thenReturn(Hand.NONE);

            PlayerBuilding otherPlayerBuilding = mock(PlayerBuilding.class);
            when(otherPlayerBuilding.getPlayer()).thenReturn(otherPlayer);

            Location.BuildingLocation a1 = mock(Location.BuildingLocation.class);
            when(a1.getHand()).thenReturn(Hand.GREEN);
            when(a1.isDirect(b)).thenReturn(true);
            when(a1.getBuilding()).thenReturn(Optional.of(otherPlayerBuilding));

            Location a = mock(Location.class);
            when(a.isDirect(a1)).thenReturn(true);

            when(currentPlayerState.getStepLimit(anyInt())).thenReturn(3);
            when(currentPlayerState.getBalance()).thenReturn(10);

            when(trail.getCurrentLocation(currentPlayer)).thenReturn(Optional.of(a));

            Action.Move move = new Action.Move(List.of(a1, b));
            move.perform(game, new Random(0));

            verify(currentPlayerState).payDollars(2);
            verify(trail).movePlayer(currentPlayer, b);
            verify(otherPlayerState).gainDollars(2);
        }
    }
}
