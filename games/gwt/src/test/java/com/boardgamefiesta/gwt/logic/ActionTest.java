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

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
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

    Player currentPlayer = new Player("Player A", PlayerColor.RED, Player.Type.HUMAN);
    Player otherPlayer = new Player("Player B", PlayerColor.BLUE, Player.Type.HUMAN);

    @Mock
    GWT game;

    @Mock
    PlayerState currentPlayerState;

    @Mock
    PlayerState otherPlayerState;

    @Mock
    RailroadTrack railroadTrack;

    @Mock
    Trail trail;

    @Mock
    ActionStack actionStack;

    @BeforeEach
    void setUp() {
        lenient().when(game.getCurrentPlayer()).thenReturn(currentPlayer);
        lenient().when(game.currentPlayerState()).thenReturn(currentPlayerState);
        lenient().when(game.getRailroadTrack()).thenReturn(railroadTrack);
        lenient().when(game.getTrail()).thenReturn(trail);
        lenient().when(game.getPlayers()).thenReturn(List.of(currentPlayer, otherPlayer));
        lenient().when(game.getPlayerOrder()).thenReturn(List.of(currentPlayer, otherPlayer));
        lenient().when(game.playerState(currentPlayer)).thenReturn(currentPlayerState);
        lenient().when(game.playerState(otherPlayer)).thenReturn(otherPlayerState);
        lenient().when(game.getActionStack()).thenReturn(actionStack);
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
            when(railroadTrack.transportCosts(currentPlayer, City.EL_PASO)).thenReturn(0);
            when(trail.atKansasCity(currentPlayer)).thenReturn(true);

            Action.DeliverToCity deliverToCity = new Action.DeliverToCity(City.EL_PASO, 1);
            deliverToCity.perform(game, new Random(0));

            verify(currentPlayerState).gainDollars(12);
            verify(currentPlayerState).discardHand(game);
            verify(trail, never()).moveToStart(currentPlayer); // done on endTurn
        }

        @Test
        void spendOnlyPermCerts() {
            // TODO
        }

        @Test
        void spendTempCerts() {
            // TODO
        }

    }

    @Nested
    class Move {

        @Test
        void move() {
            Location b = mock(Location.class);
            when(b.getName()).thenReturn("B");
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
            when(b.getName()).thenReturn("B");
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
            when(b.getName()).thenReturn("B");
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
            when(b.getName()).thenReturn("B");
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

    @Nested
    class TradeWithTribesTest {

        @Test
        void gainDollars() {
            var teepeeLocation = mock(Location.TeepeeLocation.class);
            when(teepeeLocation.getTeepee()).thenReturn(Optional.of(Teepee.BLUE));
            when(teepeeLocation.getReward()).thenReturn(1);

            new Action.TradeWithTribes(teepeeLocation).perform(game, new Random(0));

            verify(currentPlayerState).gainDollars(1);
            verify(currentPlayerState).addTeepee(Teepee.BLUE);
            verify(teepeeLocation).removeTeepee();
        }

        @Test
        void payDollars() {
            var teepeeLocation = mock(Location.TeepeeLocation.class);
            when(teepeeLocation.getTeepee()).thenReturn(Optional.of(Teepee.BLUE));
            when(teepeeLocation.getReward()).thenReturn(-1);

            new Action.TradeWithTribes(teepeeLocation).perform(game, new Random(0));

            verify(currentPlayerState).payDollars(1);
            verify(currentPlayerState).addTeepee(Teepee.BLUE);
            verify(teepeeLocation).removeTeepee();
        }

        @Test
        void notEnoughBalance() {
            var teepeeLocation = mock(Location.TeepeeLocation.class);
            when(teepeeLocation.getTeepee()).thenReturn(Optional.of(Teepee.BLUE));
            when(teepeeLocation.getReward()).thenReturn(-1);

            doThrow(new GWTException(GWTError.NOT_ENOUGH_BALANCE_TO_PAY)).when(currentPlayerState).payDollars(anyInt());

            assertThatThrownBy(() -> new Action.TradeWithTribes(teepeeLocation).perform(game, new Random(0)))
                    .isInstanceOf(GWTException.class);

            verify(currentPlayerState, never()).addTeepee(any());
            verify(teepeeLocation, never()).removeTeepee();
        }

        @Test
        void noTeepee() {
            var teepeeLocation = mock(Location.TeepeeLocation.class);
            when(teepeeLocation.getTeepee()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> new Action.TradeWithTribes(teepeeLocation).perform(game, new Random(0)))
                    .isInstanceOf(GWTException.class);

            verifyNoInteractions(currentPlayerState);
            verify(teepeeLocation, never()).removeTeepee();
        }
    }

    @Nested
    class Move3ForwardWithoutFeesTest {

        @Test
        void shouldAllowMoveWhenPlayedBeforeActivatingLocation() {
            var game = TestHelper.givenAGame();

            game.getTrail().moveToStart(game.getCurrentPlayer());

            var objectiveCard = new ObjectiveCard(ObjectiveCard.Type.MOVE_345);
            game.currentPlayerState().addCardToHand(objectiveCard);

            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.PlayObjectiveCard.class, Action.Move.class);

            game.perform(new Action.PlayObjectiveCard(objectiveCard), new Random(0));
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move3ForwardWithoutFees.class);

            game.perform(new Action.Move3ForwardWithoutFees(List.of(game.getTrail().getLocation("A"))), new Random(0));
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);

            game.perform(new Action.Move(List.of(game.getTrail().getLocation("B"))), new Random(0));
            assertThat(game.possibleActions()).isNotEmpty();
        }

        @Test
        void shouldNotAllowActionsFromPreviousLocation() {
            var game = TestHelper.givenAGame();

            var a1 = game.getTrail().getBuildingLocation("A-1").get();
            var a2 = game.getTrail().getBuildingLocation("A-2").get();
            a1.placeBuilding(new PlayerBuilding.Building4B(game.getCurrentPlayer()));
            a2.placeBuilding(new PlayerBuilding.Building4B(game.getNextPlayer()));

            var objectiveCard = new ObjectiveCard(ObjectiveCard.Type.MOVE_345);
            game.currentPlayerState().addCardToHand(objectiveCard);

            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.PlayObjectiveCard.class, Action.Move.class);

            game.perform(new Action.Move(List.of(a1)), new Random(0));
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                    Action.SingleAuxiliaryAction.class,
                    Action.PlayObjectiveCard.class,
                    Action.Move3Forward.class,
                    Action.DrawCard.class);

            game.perform(new Action.PlayObjectiveCard(objectiveCard), new Random(0));
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move3ForwardWithoutFees.class);

            game.perform(new Action.Move3ForwardWithoutFees(List.of(a2)), new Random(0));
            assertThat(game.possibleActions()).isEmpty();
        }

        @Test
        void shouldNotAllowKansasCity() {
            var game = TestHelper.givenAGame();

            var objectiveCard = new ObjectiveCard(ObjectiveCard.Type.MOVE_345);
            game.currentPlayerState().addCardToHand(objectiveCard);

            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.PlayObjectiveCard.class, Action.Move.class);

            game.perform(new Action.Move(List.of(game.getTrail().getLocation("G"))), new Random(0));
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                    Action.PlayObjectiveCard.class,
                    Action.MoveEngineForward.class,
                    Action.SingleOrDoubleAuxiliaryAction.class);

            game.perform(new Action.PlayObjectiveCard(objectiveCard), new Random(0));
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move3ForwardWithoutFees.class);

            assertThatThrownBy(() -> game.perform(new Action.Move3ForwardWithoutFees(List.of(game.getTrail().getKansasCity())), new Random(0)))
                    .isInstanceOf(GWTException.class)
                    .hasMessage(GWTError.CANNOT_PERFORM_ACTION.name());
        }
    }

}
