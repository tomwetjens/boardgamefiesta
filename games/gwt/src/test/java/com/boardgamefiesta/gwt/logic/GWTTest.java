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

import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GWTTest {

    private static final Class[] A_BUILDINGS = {
            PlayerBuilding.Building1A.class,
            PlayerBuilding.Building2A.class,
            PlayerBuilding.Building3A.class,
            PlayerBuilding.Building4A.class,
            PlayerBuilding.Building5A.class,
            PlayerBuilding.Building6A.class,
            PlayerBuilding.Building7A.class,
            PlayerBuilding.Building8A.class,
            PlayerBuilding.Building9A.class,
            PlayerBuilding.Building10A.class
    };

    private static final GWT.Options BEGINNER = GWT.Options.builder()
            .buildings(GWT.Options.Buildings.BEGINNER)
            .build();

    private static final GWT.Options RANDOMIZED = GWT.Options.builder()
            .buildings(GWT.Options.Buildings.RANDOMIZED)
            .build();

    private Player playerA = new Player("Player A", PlayerColor.WHITE, Player.Type.HUMAN);
    private Player playerB = new Player("Player B", PlayerColor.YELLOW, Player.Type.HUMAN);
    private Player playerC = new Player("Player C", PlayerColor.BLUE, Player.Type.HUMAN);
    private Player playerD = new Player("Player D", PlayerColor.RED, Player.Type.HUMAN);

    @Mock
    InGameEventListener eventListener;

    @Nested
    class Create {

        @Test
        void beginner() {
            GWT game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, eventListener, new Random(0));

            assertThat(game.getStatus()).isEqualTo(GWT.Status.STARTED);
            assertThat(game.getPlayerOrder()).containsExactly(playerA, playerB);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerA);

            // Neutral buildings should not be randomized
            assertThat(Arrays.asList(
                    ((Location.BuildingLocation) game.getTrail().getLocation("A")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("B")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("C")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("D")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("E")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("F")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("G")).getBuilding().get())).extracting(Building::getName)
                    .containsExactly("A", "B", "C", "D", "E", "F", "G");

            // Player buildings should not be randomized
            assertThat(game.currentPlayerState().getBuildings()).extracting(PlayerBuilding::getClass).containsExactlyInAnyOrder(A_BUILDINGS);

            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);
        }

        @Test
        void randomized() {
            GWT game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB)), RANDOMIZED, eventListener, new Random(0));

            assertThat(game.getStatus()).isEqualTo(GWT.Status.STARTED);
            assertThat(game.getPlayerOrder()).containsExactly(playerA, playerB);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerA);
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);

            // Neutral buildings should be randomized
            assertThat(Arrays.asList(
                    ((Location.BuildingLocation) game.getTrail().getLocation("A")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("B")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("C")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("D")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("E")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("F")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("G")).getBuilding().get())).extracting(Building::getName)
                    .doesNotContainSequence("A", "B", "C", "D", "E", "F", "G");

            // Player buildings should be randomized
            Class[] randomizedBuildings = {
                    PlayerBuilding.Building1A.class,
                    PlayerBuilding.Building2B.class,
                    PlayerBuilding.Building3A.class,
                    PlayerBuilding.Building4A.class,
                    PlayerBuilding.Building5B.class,
                    PlayerBuilding.Building6A.class,
                    PlayerBuilding.Building7B.class,
                    PlayerBuilding.Building8A.class,
                    PlayerBuilding.Building9A.class,
                    PlayerBuilding.Building10B.class
            };
            assertThat(game.currentPlayerState().getBuildings()).extracting(PlayerBuilding::getClass).containsExactlyInAnyOrder(randomizedBuildings);
        }
    }

    @Nested
    class Serialize {

        @Test
        void serialize() {
            var game = GWT.start(GWT.Edition.FIRST, new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, eventListener, new Random(0));

            // TODO
        }
    }

    @Nested
    class GetPossibleActions {

        @Test
        void noObjectiveCard() {

        }

        @Test
        void hasObjectiveCard() {

        }

        @Test
        void hasObjectiveCardButImmediateActions() {

        }
    }

    @Nested
    class PlaceDisc {

        @Test
        void needWhite() {
            GWT game = GWT.start(GWT.Edition.FIRST, new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, eventListener, new Random(0));

            assertThat(game.removeDisc(Collections.singletonList(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);
        }

        @Test
        void needBlackOrWhite() {
            GWT game = GWT.start(GWT.Edition.FIRST, new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, eventListener, new Random(0));

            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
        }

        @Test
        void needWhiteButCanOnlyUnlockBlack() {
            GWT game = GWT.start(GWT.Edition.FIRST, new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, eventListener, new Random(0));

            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);

            // Remove all white discs
            game.currentPlayerState().unlock(Unlockable.AUX_GAIN_DOLLAR);
            game.currentPlayerState().unlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_4);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);

            // Should allow removal of black by exception
            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
            // Normal case to allow black
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
        }

        @Test
        void cannotUnlockButStationsUpgraded() {
            GWT game = GWT.start(GWT.Edition.FIRST, new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, eventListener, new Random(0));
            // Enough money to pay for unlocks
            game.currentPlayerState().gainDollars(10);

            // Remove all discs
            game.currentPlayerState().unlock(Unlockable.AUX_GAIN_DOLLAR);
            game.currentPlayerState().unlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_4);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_6);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_DOLLARS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_POINTS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);

            // Place one on a station
            game.getRailroadTrack().upgradeStation(game, game.getRailroadTrack().getStations().get(0));

            // Player is forced to remove disc from station
            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.DowngradeStation.class);
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.DowngradeStation.class);
        }

        @Test
        void cannotUnlockNoStationsUpgraded() {
            GWT game = GWT.start(GWT.Edition.FIRST, new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, eventListener, new Random(0));
            // Enough money to pay for unlocks
            game.currentPlayerState().gainDollars(10);

            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);

            // Remove all discs
            game.currentPlayerState().unlock(Unlockable.AUX_GAIN_DOLLAR);
            game.currentPlayerState().unlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_4);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_6);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_DOLLARS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_POINTS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);

            // Player has no discs on stations
            // So player cannot do anything = exception case -> no immediate actions
            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions()).isEmpty();
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions()).isEmpty();
        }
    }

    @Nested
    class BiddingTest {

        @Test
        void startWhenAllPositionsUncontested() {
            var game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), eventListener, new Random(0));

            assertThat(game.getPlayerOrder()).containsExactly(playerC, playerB, playerA);

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerC, new Action.PlaceBid(new Bid(2, 0)), new Random(0));
            game.endTurn(playerC, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerB, new Action.PlaceBid(new Bid(1, 0)), new Random(0));
            game.endTurn(playerB, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerA, new Action.PlaceBid(new Bid(0, 0)), new Random(0));
            game.endTurn(playerA, new Random(0));

            assertThat(game.getStatus()).isEqualTo(GWT.Status.STARTED);
            assertThat(game.getPlayerOrder()).containsExactly(playerA, playerB, playerC);
            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);
        }

        @Test
        void canUndoBid() {
            var game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), eventListener, new Random(0));

            assertThat(game.getPlayerOrder()).containsExactly(playerC, playerB, playerA);

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerC, new Action.PlaceBid(new Bid(2, 0)), new Random(0));

            assertThat(game.canUndo()).isTrue();

            game.endTurn(playerC, new Random(0));

            assertThat(game.canUndo()).isFalse();
        }

        @Test
        void skipTurnWhenUncontested() {
            var game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), eventListener, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerC, new Action.PlaceBid(new Bid(0, 0)), new Random(0));
            game.endTurn(playerC, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerB, new Action.PlaceBid(new Bid(1, 0)), new Random(0));
            game.endTurn(playerB, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerA, new Action.PlaceBid(new Bid(1, 1)), new Random(0));
            game.endTurn(playerA, new Random(0));

            assertThat(game.getCurrentPlayer()).describedAs("should skip player C, since bid is uncontested").isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(playerB, new Action.PlaceBid(new Bid(2, 0)), new Random(0));
            game.endTurn(playerB, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);
        }

        @Test
        void forceEndTurnDuringBidding() {
            var game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), eventListener, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.forceEndTurn(playerC, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);
        }

        @Test
        void leaveDuringBidding() {
            var game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), eventListener, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.leave(playerC, new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);
        }

        @Test
        void druen() {
            // Feb 16, 2022
            // https://boardgamefiesta.com/gwt2/c4a5026c-adcf-4d92-8c46-dd1bdb0adf3e

            var game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC, playerD)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), eventListener, new Random(0));

            // playerA = mario
            // playerB = hildegard
            // playerC = dannerz
            // playerD = druen

            // druen bids -1VP for seat #1
            assertThat(game.getCurrentPlayer()).isSameAs(playerD);
            game.perform(new Action.PlaceBid(new Bid(1, 1)), new Random(0));
            game.endTurn(playerD, new Random(0));

            // mario bids 0VP for seat #3
            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            game.perform(new Action.PlaceBid(new Bid(3, 0)), new Random(0));
            game.endTurn(playerA, new Random(0));

            // hildegard bids 0VP for seat #2
            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            game.perform(new Action.PlaceBid(new Bid(2, 0)), new Random(0));
            game.endTurn(playerB, new Random(0));

            // dannerz bids -2VP for seat #1
            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            game.perform(new Action.PlaceBid(new Bid(1, 2)), new Random(0));
            game.endTurn(playerC, new Random(0));
            // druen is now contested

            // druen bids -3VP for seat #1
            assertThat(game.getCurrentPlayer()).isSameAs(playerD);
            game.perform(new Action.PlaceBid(new Bid(1, 3)), new Random(0));
            game.endTurn(playerD, new Random(0));
            // dannerz is now contested

            // dannerz bids -4VP for seat #1
            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            game.perform(new Action.PlaceBid(new Bid(1, 4)), new Random(0));
            game.endTurn(playerC, new Random(0));
            // druen is now contested

            // druen bids -1VP for seat #2
            assertThat(game.getCurrentPlayer()).isSameAs(playerD);
            game.perform(new Action.PlaceBid(new Bid(2, 1)), new Random(0));
            game.endTurn(playerD, new Random(0));
            // hildegard is now contested

            // hildegard bids -2VP for seat #2
            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            game.perform(new Action.PlaceBid(new Bid(2, 2)), new Random(0));
            game.endTurn(playerB, new Random(0));
            // druen is now contested

            // druen bids -4VP for seat #3
            assertThat(game.getCurrentPlayer()).isSameAs(playerD);
            game.perform(new Action.PlaceBid(new Bid(3, 4)), new Random(0));
            game.endTurn(playerD, new Random(0));
            // mario is now contested

            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);
        }
    }

    @Nested
    class UpgradeSimmentalTest {

        GWT game;

        @BeforeEach
        void setUp() {
            game = GWT.start(GWT.Edition.SECOND, new LinkedHashSet<>(Arrays.asList(playerA, playerB)), GWT.Options.builder()
                    .simmental(true)
                    .build(), eventListener, new Random(0));

            game.perform(new Action.Move(List.of(game.getTrail().getLocation("G"))), new Random(0));
            game.endTurn(game.getCurrentPlayer(), new Random(0));
            game.perform(new Action.DiscardCard(game.currentPlayerState().getHand().iterator().next()), new Random(0));
            game.perform(new Action.Move(List.of(game.getTrail().getLocation("G"))), new Random(0));
            game.endTurn(game.getCurrentPlayer(), new Random(0));
        }

        @Test
        void shouldDiscardCardsImmediatelyWhenNoSimmentalInHand() {
            // Make sure the hand does not have a Simmental for this test case
            assertThat(game.currentPlayerState().simmentalsToUpgrade()).isEqualTo(0);

            // When
            game.perform(new Action.Move(List.of(game.getTrail().getKansasCity())), new Random(0));
            game.perform(new Action.ChooseForesight1(0), new Random(0));
            game.perform(new Action.ChooseForesight2(0), new Random(0));
            game.perform(new Action.ChooseForesight3(0), new Random(0));
            game.perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), new Random(0));

            // Then
            assertThat(game.currentPlayerState().getHand()).isEmpty();
        }

        @Test
        void shouldDiscardCardsImmediatelyWhenNoSimmentalInHandToUpgrade() {
            // Put a fully upgraded Simmental in the hand of the player
            var simmentalCard = new Card.CattleCard(CattleType.SIMMENTAL, 5, 5);
            game.currentPlayerState().addCardToHand(simmentalCard);

            // When
            game.perform(new Action.Move(List.of(game.getTrail().getKansasCity())), new Random(0));
            game.perform(new Action.ChooseForesight1(0), new Random(0));
            game.perform(new Action.ChooseForesight2(0), new Random(0));
            game.perform(new Action.ChooseForesight3(0), new Random(0));
            game.perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), new Random(0));

            // Then
            assertThat(game.currentPlayerState().getHand()).isEmpty();
        }

        @Test
        void shouldDiscardCardsAfterUpgrading() {
            // Put a Simmental in the hand of the player
            var simmentalCard = new Card.CattleCard(CattleType.SIMMENTAL, 3, 2);
            game.currentPlayerState().addCardToHand(simmentalCard);

            // When
            game.perform(new Action.Move(List.of(game.getTrail().getKansasCity())), new Random(0));
            game.perform(new Action.ChooseForesight1(0), new Random(0));
            game.perform(new Action.ChooseForesight2(0), new Random(0));
            game.perform(new Action.ChooseForesight3(0), new Random(0));
            game.perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), new Random(0));
            game.perform(new Action.UnlockWhite(Unlockable.AUX_GAIN_DOLLAR), new Random(0));
            game.perform(new Action.UpgradeSimmental(simmentalCard), new Random(0));

            // Then
            assertThat(game.currentPlayerState().getHand()).isEmpty();
        }

        @Test
        void shouldDiscardCardsAfterUpgradingMultiple() {
            // Put multiple Simmentals in the hand of the player
            var simmentalCard1 = new Card.CattleCard(CattleType.SIMMENTAL, 3, 2);
            game.currentPlayerState().addCardToHand(simmentalCard1);
            var simmentalCard2 = new Card.CattleCard(CattleType.SIMMENTAL, 3, 2);
            game.currentPlayerState().addCardToHand(simmentalCard2);

            // When
            game.perform(new Action.Move(List.of(game.getTrail().getKansasCity())), new Random(0));
            game.perform(new Action.ChooseForesight1(0), new Random(0));
            game.perform(new Action.ChooseForesight2(0), new Random(0));
            game.perform(new Action.ChooseForesight3(0), new Random(0));
            game.perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), new Random(0));
            game.perform(new Action.UnlockWhite(Unlockable.AUX_GAIN_DOLLAR), new Random(0));
            game.perform(new Action.UpgradeSimmental(simmentalCard1), new Random(0));
            game.perform(new Action.UpgradeSimmental(simmentalCard2), new Random(0));

            // Then
            assertThat(game.currentPlayerState().getHand()).isEmpty();
        }

        @Test
        void shouldDiscardCardsAfterSkip() {
            // Put a Simmental in the hand of the player
            var simmentalCard = new Card.CattleCard(CattleType.SIMMENTAL, 3, 2);
            game.currentPlayerState().addCardToHand(simmentalCard);

            // When
            game.perform(new Action.Move(List.of(game.getTrail().getKansasCity())), new Random(0));
            game.perform(new Action.ChooseForesight1(0), new Random(0));
            game.perform(new Action.ChooseForesight2(0), new Random(0));
            game.perform(new Action.ChooseForesight3(0), new Random(0));
            game.perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), new Random(0));
            game.perform(new Action.UnlockWhite(Unlockable.AUX_GAIN_DOLLAR), new Random(0));
            game.skip(new Random(0));

            // Then
            assertThat(game.currentPlayerState().getHand()).isEmpty();
        }

        @Test
        void shouldDiscardCardsWhenEndTurn() {
            // Put a Simmental in the hand of the player
            var player = game.getCurrentPlayer();
            var simmentalCard = new Card.CattleCard(CattleType.SIMMENTAL, 3, 2);
            game.playerState(player).addCardToHand(simmentalCard);

            // When
            game.perform(new Action.Move(List.of(game.getTrail().getKansasCity())), new Random(0));
            game.perform(new Action.ChooseForesight1(0), new Random(0));
            game.perform(new Action.ChooseForesight2(0), new Random(0));
            game.perform(new Action.ChooseForesight3(0), new Random(0));
            game.perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), new Random(0));
            game.perform(new Action.UnlockWhite(Unlockable.AUX_GAIN_DOLLAR), new Random(0));
            assertThat(game.possibleActions()).contains(Action.UpgradeSimmental.class);
            game.endTurn(player, new Random(0));

            // Then
            assertThat(game.playerState(player).getHand()).doesNotContain(simmentalCard);
        }
    }


    @Nested
    class DowngradeStationTest {

        GWT game;

        @BeforeEach
        void setUp() {
            game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB)), GWT.Options.builder()
                    .buildings(GWT.Options.Buildings.BEGINNER)
                    .build(), eventListener, new Random(0));

            var currentPlayerState = game.currentPlayerState();

            // Simulate that player has maxed out on engineers to be able to reach the first station
            IntStream.range(0, 5).forEach(i -> currentPlayerState.gainWorker(Worker.ENGINEER, game));

            game.perform(new Action.Move(List.of(game.getTrail().getLocation("C"))), new Random(0));
        }

        void removedAllDiscs(PlayerState currentPlayerState) {
            // Simulate that player as removed all discs from the player board
            Arrays.stream(Unlockable.values()).forEach(unlockable -> {
                while (currentPlayerState.canUnlock(unlockable, game)) {
                    currentPlayerState.unlock(unlockable);
                }
            });
        }

        @Test
        void downgradeStationWhenUpgradingStation() {
            // Given
            removedAllDiscs(game.currentPlayerState());
            game.getRailroadTrack().upgradeStation(game, RailroadTrack.STATION2);

            // When
            game.perform(new Action.MoveEngineForward(game.getRailroadTrack().getSpace(RailroadTrack.STATION1)), new Random(0));
            game.perform(new Action.UpgradeStation(), new Random(0));
            game.perform(new Action.DowngradeStation(RailroadTrack.STATION2), new Random(0));

            // Then
            assertThat(game.getRailroadTrack().getUpgradedBy(RailroadTrack.STATION1)).containsExactly(game.getCurrentPlayer());
            assertThat(game.getRailroadTrack().getUpgradedBy(RailroadTrack.STATION2)).isEmpty();
        }

        @Test
        void mustDowngradeDifferentStationFromLastUpgradedInSameTurn() {
            // Given
            removedAllDiscs(game.currentPlayerState());

            // When
            game.perform(new Action.MoveEngineForward(game.getRailroadTrack().getSpace(RailroadTrack.STATION1)), new Random(0));
            game.perform(new Action.UpgradeStation(), new Random(0));

            assertThatThrownBy(() -> new Action.DowngradeStation(RailroadTrack.STATION1).perform(game, new Random(0)))
                    .isInstanceOf(GWTException.class)
                    .hasMessage(GWTError.STATION_MUST_BE_DIFFERENT.name());
        }

        @Test
        void mayDowngradeStationUpgradedInPreviousTurn() {
            // Given
            game.perform(new Action.MoveEngineForward(game.getRailroadTrack().getSpace(RailroadTrack.STATION1)), new Random(0));
            game.perform(new Action.UpgradeStation(), new Random(0));
            game.perform(new Action.UnlockWhite(Unlockable.AUX_GAIN_DOLLAR), new Random(0));
            removedAllDiscs(game.currentPlayerState());
            game.endTurn(game.getCurrentPlayer(), new Random(0));

            game.perform(new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(game.getCurrentPlayer(), new Random(0));

            // When
            game.perform(new Action.Move(List.of(
                    game.getTrail().getLocation("E"),
                    game.getTrail().getLocation("F"),
                    game.getTrail().getLocation("G"))), new Random(0));
            game.perform(new Action.MoveEngineForward(game.getRailroadTrack().getSpace(RailroadTrack.STATION2)), new Random(0));
            game.perform(new Action.UpgradeStation(), new Random(0));
            game.perform(new Action.DowngradeStation(RailroadTrack.STATION1), new Random(0));
        }

    }

    @Nested
    class Leave {

        GWT game;

        @BeforeEach
        void setUp() {
            game = GWT.start(GWT.Edition.FIRST, new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .buildings(GWT.Options.Buildings.BEGINNER)
                    .build(), eventListener, new Random(0));

            var currentPlayerState = game.currentPlayerState();

            // Simulate that job market is closed and player has taken job market token
            while (!game.getJobMarket().isClosed()) {
                game.getJobMarket().addWorker(Worker.COWBOY);
            }
            currentPlayerState.gainJobMarketToken();
        }

        @Test
        void leaveAfterGainingJobMarketTokenBeforeEndTurn() {
            assertThat(game.getPlayerOrder()).containsExactly(playerC, playerB, playerA);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerC);

            game.leave(playerC, new Random(0));

            assertThat(game.getPlayerOrder()).containsExactly(playerB, playerA);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerB);

            // Then other players complete last turns as usual
            game.perform(playerB, new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(playerB, new Random(0));

            game.perform(playerA, new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(playerA, new Random(0));

            // It should be ended now
            assertThat(game.isEnded()).isTrue();
        }

        @Test
        void leaveAfterGainingJobMarketTokenAfterEndTurn() {
            assertThat(game.getPlayerOrder()).containsExactly(playerC, playerB, playerA);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerC);

            game.perform(playerC, new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(playerC, new Random(0));
            game.leave(playerC, new Random(0));

            assertThat(game.getPlayerOrder()).containsExactly(playerB, playerA);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerB);

            // Then other players complete last turns as usual
            game.perform(new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(playerB, new Random(0));

            game.perform(new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(playerA, new Random(0));

            // It should be ended now
            assertThat(game.isEnded()).isTrue();
        }

        @Test
        void leaveOnLastTurn() {
            assertThat(game.getPlayerOrder()).containsExactly(playerC, playerB, playerA);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerC);

            // Players - expect last - complete last turns as usual
            game.perform(playerC, new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(playerC, new Random(0));
            game.perform(playerB, new Action.Move(List.of(game.getTrail().getLocation("A"))), new Random(0));
            game.endTurn(playerB, new Random(0));

            // Leave during last turn of the game
            game.leave(playerA, new Random(0));

            // It should be ended now
            assertThat(game.isEnded()).isTrue();
        }
    }
}
