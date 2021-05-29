package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class IstanbulTest {

    private Player playerRed = new Player("Red", PlayerColor.RED, Player.Type.HUMAN);
    private Player playerGreen = new Player("Green", PlayerColor.GREEN, Player.Type.HUMAN);
    private Player playerBlue = new Player("Blue", PlayerColor.BLUE, Player.Type.HUMAN);

    @Nested
    class BonusCards {

        @Nested
        class Take5Lira {

            @Test
            void availableBeforeMove() {
                // Given
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));

                // When
                game.currentPlayerState().addBonusCard(BonusCard.TAKE_5_LIRA);

                // Then
                assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Move.class, Action.BonusCardTake5Lira.class);
            }

            @Test
            void perform() {
                // Given
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));

                // When
                game.currentPlayerState().addBonusCard(BonusCard.GAIN_1_GOOD);

                // Then
                assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Move.class, Action.BonusCardGain1Good.class);
            }

            @Test
            void perform() {
                // Given
                var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            game.getPoliceStation().takeSmuggler();
            game.getSpiceWarehouse().placeSmuggler();
            game.getSpiceWarehouse().takeGovernor();
            game.getLargeMarket().placeGovernor();

            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.perform(new Action.SendFamilyMember(game.getSpiceWarehouse()), new Random(0));
            game.perform(new Action.MaxSpice(), new Random(0));

            // When
            assertThat(game.getPossibleActions()).containsExactly(Action.Smuggler.class);
            game.perform(new Action.Smuggler(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Fruit.class,
                    Action.Take1Spice.class, Action.Take1Fabric.class, Action.Take1Blue.class);
        }

        @Test
        void smugglerAfterSkipSendFamilyMember() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            game.getPoliceStation().takeSmuggler();
            game.getSpiceWarehouse().placeSmuggler();
            game.getSpiceWarehouse().takeGovernor();
            game.getLargeMarket().placeGovernor();

            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.perform(new Action.SendFamilyMember(game.getLargeMarket()), new Random(0));

            // When
            game.skip(new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactly(Action.Governor.class);
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.LONG_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen, playerBlue)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen, playerBlue)), LayoutType.SHORT_PATHS, new Random(0));
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
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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
        var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
        assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
        game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
        game.getPlayerState(playerGreen).gainRubies(3);
        game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
        game.endTurn(new Random(0));
        game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
        game.endTurn(new Random(0));

        assertThat(game.score(playerRed)).contains(6);
        assertThat(game.score(playerGreen)).contains(3);
    }

    @Nested
    class Winners {

        @Test
        void winnerRubies() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerGreen).gainRubies(3);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.ranking()).containsExactly(playerRed, playerGreen);
        }

        @Test
        void winnerLira() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            assertThat(game.getPlayers()).containsExactly(playerRed, playerGreen);
            game.getPlayerState(playerRed).gainRubies(game.getMaxRubies());
            game.getPlayerState(playerGreen).gainRubies(game.getMaxRubies());
            assertThat(game.getPlayerState(playerRed).getLira()).isEqualTo(2);
            assertThat(game.getPlayerState(playerGreen).getLira()).isEqualTo(3);
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));
            game.perform(new Action.Move(game.getSpiceWarehouse()), new Random(0));
            game.endTurn(new Random(0));

            assertThat(game.ranking()).containsExactly(playerGreen, playerRed);
        }

        @Test
        void winnerTotalGoods() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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

            assertThat(game.ranking()).containsExactly(playerRed, playerGreen);
        }

        @Test
        void winnerBonusCards() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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

            assertThat(game.ranking()).containsExactly(playerGreen, playerRed);
        }

        @Test
        void winners() {
            // Given
            var game = Istanbul.start(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
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

            assertThat(game.ranking()).containsExactly(playerGreen, playerRed);
        }
    }
}