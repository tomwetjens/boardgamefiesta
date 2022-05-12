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

package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IstanbulTest {

    private Player playerRed = new Player("Red", PlayerColor.RED, Player.Type.HUMAN);
    private Player playerGreen = new Player("Green", PlayerColor.GREEN, Player.Type.HUMAN);
    private Player playerBlue = new Player("Blue", PlayerColor.BLUE, Player.Type.HUMAN);

    @Mock
    InGameEventListener eventListener;

    @Nested
    class BonusCards {

        @Nested
        class Take5Lira {

            @Test
            void availableBeforeMove() {
                // Given
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));

                // When
                game.currentPlayerState().addBonusCard(BonusCard.TAKE_5_LIRA);

                // Then
                assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Move.class, Action.BonusCardTake5Lira.class);
            }

            @Test
            void perform() {
                // Given
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
                game.currentPlayerState().addBonusCard(BonusCard.TAKE_5_LIRA);

                // When
                game.perform(new Action.BonusCardTake5Lira(), new Random(0));

                // Then
                assertThat(game.currentPlayerState().getLira()).isEqualTo(7);
            }
        }

        @Nested
        class Gain1Good {

            @Test
            void availableBeforeMove() {
                // Given
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));

                // When
                game.currentPlayerState().addBonusCard(BonusCard.GAIN_1_GOOD);

                // Then
                assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Move.class, Action.BonusCardGain1Good.class);
            }

            @Test
            void perform() {
                // Given
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
                game.currentPlayerState().addBonusCard(BonusCard.GAIN_1_GOOD);

                // When
                game.perform(new Action.BonusCardGain1Good(), new Random(0));

                // Then
                assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(
                        Action.Take1Fabric.class,
                        Action.Take1Spice.class,
                        Action.Take1Fruit.class,
                        Action.Take1Blue.class);
            }
        }
    }

    @Nested
    class PoliceStation {

        @Test
        void leaveAssistantWithSmuggler() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));

            // When
            game.perform(new Action.LeaveAssistant(), new Random(0));

            // Then
            assertThat(game.getPoliceStation().getMerchants()).extracting(Merchant::getColor).containsExactly(PlayerColor.RED);
            assertThat(game.getPoliceStation().getAssistants()).containsEntry(PlayerColor.RED, 1);
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.SendFamilyMember.class);
        }

        @Test
        void smugglerAfterPerformSendFamilyMember() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.getLayout().place(Place::isSmuggler).takeSmuggler();
            game.getSpiceWarehouse().placeSmuggler();
            game.getLayout().place(Place::isGovernor).takeGovernor();
            game.getLargeMarket().placeGovernor();

            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.perform(new Action.SendFamilyMember(game.getSpiceWarehouse()), new Random(0));

            // When
            game.perform(new Action.MaxSpice(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).isEmpty();
        }

        @Test
        void smugglerAfterSkipSendFamilyMember() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.skip(new Random(0));

            // When
            assertThat(game.getPossibleActions()).containsExactly(Action.Smuggler.class);
            game.perform(new Action.Smuggler(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Fruit.class,
                    Action.Take1Spice.class, Action.Take1Fabric.class, Action.Take1Blue.class);
        }

        @Test
        void sendFamilyMember() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));

            // When
            game.perform(new Action.SendFamilyMember(game.getSpiceWarehouse()), new Random(0));

            // Then
            assertThat(game.getPoliceStation().getFamilyMembers()).containsExactly(playerGreen);
            assertThat(game.getSpiceWarehouse().getFamilyMembers()).containsExactly(playerRed);
            assertThat(game.getPossibleActions()).containsExactly(Action.MaxSpice.class);
        }

        @Test
        void sendFamilyMemberToPlaceWithGovernor() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.getLayout().place(Place::isSmuggler).takeSmuggler();
            game.getSpiceWarehouse().placeSmuggler();
            game.getLayout().place(Place::isGovernor).takeGovernor();
            game.getLargeMarket().placeGovernor();

            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.perform(new Action.SendFamilyMember(game.getLargeMarket()), new Random(0));

            // When
            game.skip(new Random(0));

            // Then
            assertThat(game.getPossibleActions()).doesNotContain(Action.Governor.class, Action.Smuggler.class);
        }

        @Test
        void sendFamilyMemberToPlaceWithOtherFamilyMember() {
            // TODO
        }

    }

    @Test
    void catchFamilyMember() {
        // TODO
    }

    @Test
    void catchFamilyMembers() {
        // TODO
    }

    @Test
    void catchFamilyMembersAtPlaceWithGovernor() {
        // TODO
    }

    @Test
    void catchFamilyMembersAtPlaceWithSmuggler() {
        // TODO
    }

    @Test
    void catchFamilyMembersAtPlaceWithSmugglerAndGovernor() {
        // TODO
    }

    @Nested
    class BlackMarket {

        @Test
        void rollOrTakeBothAvailable() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, eventListener, new Random(0));
            game.perform(new Action.Move(game.getBlackMarket()), new Random(0));

            // When
            game.perform(new Action.LeaveAssistant(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(
                    Action.RollForBlueGoods.class,
                    Action.Take1Spice.class,
                    Action.Take1Fruit.class,
                    Action.Take1Fabric.class);
        }

        @Test
        void rollFirst() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, eventListener, new Random(0));
            game.perform(new Action.Move(game.getBlackMarket()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));

            // When
            game.perform(new Action.RollForBlueGoods(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(
                    Action.Take1Spice.class,
                    Action.Take1Fruit.class,
                    Action.Take1Fabric.class);
        }

        @Test
        void takeGoodFirst() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, eventListener, new Random(0));
            game.perform(new Action.Move(game.getBlackMarket()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));

            // When
            game.perform(new Action.Take1Fabric(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.RollForBlueGoods.class);
        }

        @Test
        void complete() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, eventListener, new Random(0));
            game.perform(new Action.Move(game.getBlackMarket()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.perform(new Action.Take1Fabric(), new Random(0));

            // When
            game.perform(new Action.RollForBlueGoods(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).contains(Action.Move.class);
        }
    }

    @Nested
    class End {

        @Test
        void lastRoundTriggeredByStartPlayer() {
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.getPlayers().stream()
                    .map(game::getPlayerState)
                    .forEach(playerState -> playerState.getBonusCards().forEach(playerState::playBonusCard));

            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());

            assertThat(game.getCurrentPlayer()).isSameAs(playerRed);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            // Last round

            assertThat(game.isEnded()).isFalse();
            assertThat(game.getCurrentPlayer()).isSameAs(playerGreen);

            game.perform(new Action.Move(game.getSmallMarket()), new Random(0));
            game.endTurn(new Random(0));
            // Ends game since players have no bonus cards left

            assertThat(game.isEnded()).isTrue();
        }

        @Test
        void lastRoundTriggeredByStartPlayerAndPlayBonusCards() {
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.getPlayers().stream()
                    .map(game::getPlayerState)
                    .forEach(playerState -> playerState.getBonusCards().forEach(playerState::playBonusCard));

            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerRed).addBonusCard(BonusCard.TAKE_5_LIRA);
            game.getPlayerState(playerRed).addBonusCard(BonusCard.GAIN_1_GOOD);
            game.getPlayerState(playerRed).addBonusCard(BonusCard.RETURN_1_ASSISTANT);
            game.getPlayerState(playerGreen).addBonusCard(BonusCard.TAKE_5_LIRA);
            game.getPlayerState(playerGreen).addBonusCard(BonusCard.GAIN_1_GOOD);
            game.getPlayerState(playerGreen).addBonusCard(BonusCard.RETURN_1_ASSISTANT);

            assertThat(game.getCurrentPlayer()).isSameAs(playerRed);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            // Last round

            assertThat(game.isEnded()).isFalse();
            assertThat(game.getCurrentPlayer()).isSameAs(playerGreen);

            game.perform(new Action.Move(game.getSmallMarket()), new Random(0));
            game.endTurn(new Random(0));

            // Red can still play certain bonus cards
            assertThat(game.getCurrentPlayer()).isSameAs(playerRed);
            assertThat(game.getPossibleActions()).contains(Action.BonusCardTake5Lira.class, Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardTake5Lira(), new Random(0));
            assertThat(game.getPossibleActions()).contains(Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardGain1Good(), new Random(0));
            assertThat(game.getPossibleActions()).contains(Action.Take1Blue.class, Action.Take1Spice.class, Action.Take1Fruit.class, Action.Take1Fabric.class);
            game.perform(new Action.Take1Fabric(), new Random(0));
            game.endTurn(new Random(0));

            // Green can still play certain bonus cards
            assertThat(game.getCurrentPlayer()).isSameAs(playerGreen);
            assertThat(game.getPossibleActions()).contains(Action.BonusCardTake5Lira.class, Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardTake5Lira(), new Random(0));
            assertThat(game.getPossibleActions()).contains(Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardGain1Good(), new Random(0));
            assertThat(game.getPossibleActions()).contains(Action.Take1Blue.class, Action.Take1Spice.class, Action.Take1Fruit.class, Action.Take1Fabric.class);
            game.perform(new Action.Take1Fabric(), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.isEnded()).isTrue();
        }

        @Test
        void lastRoundTriggeredByNonStartOrLastPlayer() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen, playerBlue)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.getPlayers().stream()
                    .map(game::getPlayerState)
                    .forEach(playerState -> playerState.getBonusCards().forEach(playerState::playBonusCard));

            assertThat(game.getPlayerOrder()).containsExactly(playerBlue, playerGreen, playerRed);

            assertThat(game.getCurrentPlayer()).isSameAs(playerBlue);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            // When
            assertThat(game.getCurrentPlayer()).isSameAs(playerGreen);
            game.getPlayerState(playerGreen).gainRubies(game.getMaxRubies());
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            // Then
            assertThat(game.isEnded()).isFalse();
            assertThat(game.getCurrentPlayer()).isSameAs(playerRed);
            game.perform(new Action.Move(game.getSmallMarket()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.isEnded()).isTrue();
        }


        @Test
        void lastRoundTriggeredByNonStartOrLastPlayerAndPlayBonusCards() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen, playerBlue)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.getPlayers().stream()
                    .map(game::getPlayerState)
                    .forEach(playerState -> playerState.getBonusCards().forEach(playerState::playBonusCard));

            assertThat(game.getPlayerOrder()).containsExactly(playerBlue, playerGreen, playerRed);
            game.getPlayerState(playerRed).addBonusCard(BonusCard.TAKE_5_LIRA);
            game.getPlayerState(playerRed).addBonusCard(BonusCard.GAIN_1_GOOD);
            game.getPlayerState(playerRed).addBonusCard(BonusCard.RETURN_1_ASSISTANT);
            game.getPlayerState(playerGreen).addBonusCard(BonusCard.TAKE_5_LIRA);
            game.getPlayerState(playerGreen).addBonusCard(BonusCard.GAIN_1_GOOD);
            game.getPlayerState(playerGreen).addBonusCard(BonusCard.RETURN_1_ASSISTANT);
            game.getPlayerState(playerBlue).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerBlue).addBonusCard(BonusCard.TAKE_5_LIRA);
            game.getPlayerState(playerBlue).addBonusCard(BonusCard.GAIN_1_GOOD);
            game.getPlayerState(playerBlue).addBonusCard(BonusCard.RETURN_1_ASSISTANT);

            assertThat(game.getCurrentPlayer()).isSameAs(playerBlue);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            // Last round
            assertThat(game.getCurrentPlayer()).isSameAs(playerGreen);
            game.perform(new Action.Move(game.getSmallMarket()), new Random(0));
            game.endTurn(new Random(0));
            assertThat(game.getCurrentPlayer()).isSameAs(playerRed);
            game.perform(new Action.Move(game.getSmallMarket()), new Random(0));
            game.endTurn(new Random(0));

            // Blue can still play certain bonus cards
            assertThat(game.getCurrentPlayer()).isSameAs(playerBlue);
            assertThat(game.getPossibleActions()).contains(Action.BonusCardTake5Lira.class, Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardTake5Lira(), new Random(0));
            assertThat(game.getPossibleActions()).contains(Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardGain1Good(), new Random(0));
            assertThat(game.getPossibleActions()).contains(Action.Take1Blue.class, Action.Take1Spice.class, Action.Take1Fruit.class, Action.Take1Fabric.class);
            game.perform(new Action.Take1Fabric(), new Random(0));
            game.endTurn(new Random(0));

            // Green can still play certain bonus cards
            assertThat(game.getCurrentPlayer()).isSameAs(playerGreen);
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.BonusCardTake5Lira.class, Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardTake5Lira(), new Random(0));
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardGain1Good(), new Random(0));
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Blue.class, Action.Take1Spice.class, Action.Take1Fruit.class, Action.Take1Fabric.class);
            game.perform(new Action.Take1Fabric(), new Random(0));
            game.endTurn(new Random(0));

            // Red can still play certain bonus cards
            assertThat(game.getCurrentPlayer()).isSameAs(playerRed);
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.BonusCardTake5Lira.class, Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardTake5Lira(), new Random(0));
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.BonusCardGain1Good.class);
            game.perform(new Action.BonusCardGain1Good(), new Random(0));
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Blue.class, Action.Take1Spice.class, Action.Take1Fruit.class, Action.Take1Fabric.class);
            game.perform(new Action.Take1Fabric(), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.isEnded()).isTrue();
        }

        @Test
        void lastRoundTriggeredByLastPlayer() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            game.getPlayers().stream()
                    .map(game::getPlayerState)
                    .forEach(playerState -> playerState.getBonusCards().forEach(playerState::playBonusCard));

            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);

            assertThat(game.getCurrentPlayer()).isSameAs(playerRed);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            // When
            assertThat(game.getCurrentPlayer()).isSameAs(playerGreen);
            game.getPlayerState(playerGreen).gainRubies(game.getMaxRubies());
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            // Then
            assertThat(game.isEnded()).isTrue();
        }
    }

    @Test
    void score() {
        // Given
        var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
        assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
        game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
        game.getPlayerState(playerGreen).gainRubies(3);
        game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
        game.endTurn(new Random(0));
        game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
        game.endTurn(new Random(0));

        assertThat(game.getScore(playerRed)).isEqualTo(6);
        assertThat(game.getScore(playerGreen)).isEqualTo(3);
    }

    @Nested
    class Winners {

        @Test
        void winnerRubies() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerGreen).gainRubies(3);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.getRanking()).containsExactly(playerRed, playerGreen);
        }

        @Test
        void winnerLira() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerGreen).gainRubies(game.getMaxRubies());
            assertThat(game.getPlayerState(playerRed).getLira()).isEqualTo(2);
            assertThat(game.getPlayerState(playerGreen).getLira()).isEqualTo(3);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.getRanking()).containsExactly(playerGreen, playerRed);
        }

        @Test
        void winnerTotalGoods() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerRed).gainLira(1);
            game.getPlayerState(playerRed).addGoods(GoodsType.FABRIC, 2);
            game.getPlayerState(playerRed).addGoods(GoodsType.FRUIT, 2);
            game.getPlayerState(playerGreen).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerGreen).addGoods(GoodsType.FABRIC, 1);
            game.getPlayerState(playerGreen).addGoods(GoodsType.SPICE, 2);
            assertThat(game.getPlayerState(playerRed).getLira()).isEqualTo(3);
            assertThat(game.getPlayerState(playerGreen).getLira()).isEqualTo(3);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.getRanking()).containsExactly(playerRed, playerGreen);
        }

        @Test
        void winnerBonusCards() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerRed).gainLira(1);
            game.getPlayerState(playerGreen).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerGreen).addBonusCard(BonusCard.GEMSTONE_DEALER_2X);
            assertThat(game.getPlayerState(playerRed).getLira()).isEqualTo(3);
            assertThat(game.getPlayerState(playerGreen).getLira()).isEqualTo(3);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.getRanking()).containsExactly(playerGreen, playerRed);
        }

        @Test
        void winners() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerRed).gainLira(1);
            game.getPlayerState(playerGreen).gainRubies(game.getMaxRubies());
            assertThat(game.getPlayerState(playerRed).getLira()).isEqualTo(3);
            assertThat(game.getPlayerState(playerGreen).getLira()).isEqualTo(3);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.getRanking()).containsExactly(playerGreen, playerRed);
        }
    }

    @Test
    void smugglerThenGovernor() {
        // Given
        var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
        game.getLayout().place(Place::isGovernor).takeGovernor();
        game.getPoliceStation().placeGovernor();


        // When
        assertThat(game.getPoliceStation().isGovernor()).isTrue();
        assertThat(game.getPoliceStation().isSmuggler()).isTrue();
        game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
        game.perform(new Action.LeaveAssistant(), new Random(0));

        // Then
        // Player should perform or skip place action first
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.SendFamilyMember.class);
        game.skip(new Random(0));

        // Player should be able to use governor or smuggler in any order
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Smuggler.class, Action.Governor.class);
        // When player uses Smuggler before Governor
        game.perform(new Action.Smuggler(), new Random(0));
        // It should first complete all the actions for Smuggler, before using the Governor
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Fabric.class, Action.Take1Fruit.class, Action.Take1Spice.class, Action.Take1Blue.class);
        game.perform(new Action.Take1Fruit(), new Random(0));
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Pay1Fabric.class, Action.Pay1Fruit.class, Action.Pay1Spice.class, Action.Pay1Blue.class, Action.Pay2Lira.class);
        game.perform(new Action.Pay2Lira(), new Random(0));
        game.perform(new Action.MoveSmuggler(), new Random(0));
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Governor.class);
        // Then player can use Governor
        game.perform(new Action.Governor(), new Random(0));
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Pay2Lira.class, Action.DiscardBonusCard.class);
    }

    @Test
    void governorThenSmuggler() {
        // Given
        var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, eventListener, new Random(0));
        game.getLayout().place(Place::isGovernor).takeGovernor();
        game.getPoliceStation().placeGovernor();


        // When
        assertThat(game.getPoliceStation().isGovernor()).isTrue();
        assertThat(game.getPoliceStation().isSmuggler()).isTrue();
        game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
        game.perform(new Action.LeaveAssistant(), new Random(0));

        // Then
        // Player should perform or skip place action first
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.SendFamilyMember.class);
        game.skip(new Random(0));

        // Player should be able to use governor or smuggler in any order
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Smuggler.class, Action.Governor.class);
        // When player uses Governor before Smuggler
        game.perform(new Action.Governor(), new Random(0));
        // It should first complete all the actions for Governor, before using the Smuggler
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Pay2Lira.class, Action.DiscardBonusCard.class);
        game.perform(new Action.Pay2Lira(), new Random(0));
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Smuggler.class);
        // Then player can use Smuggler
        game.perform(new Action.Smuggler(), new Random(0));
        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Fabric.class, Action.Take1Fruit.class, Action.Take1Spice.class, Action.Take1Blue.class);
    }
}