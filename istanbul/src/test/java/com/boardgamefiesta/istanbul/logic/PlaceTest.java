package com.boardgamefiesta.istanbul.logic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceTest {

    @Nested
    class SultansPalace {

        @Mock
        PlayerState playerState;

        @Test
        void initialUncovered() {
            assertThat(Place.SultansPalace.forPlayerCount(2).getUncovered()).isEqualTo(5);
            assertThat(Place.SultansPalace.forPlayerCount(3).getUncovered()).isEqualTo(5);
            assertThat(Place.SultansPalace.forPlayerCount(4).getUncovered()).isEqualTo(4);
            assertThat(Place.SultansPalace.forPlayerCount(5).getUncovered()).isEqualTo(4);
        }

        @Test
        void notEnoughGoods() {
            var sultansPalace = Place.SultansPalace.withUncovered(5);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(playerState))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NOT_ENOUGH_GOODS.name());
        }

        @Test
        void oneAnyGood() {
            var sultansPalace = Place.SultansPalace.withUncovered(5);

            when(playerState.hasAtLeastGoods(GoodsType.FABRIC, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.SPICE, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.FRUIT, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.BLUE, 1)).thenReturn(true);
            when(playerState.getTotalGoods()).thenReturn(5);

            var actionResult = sultansPalace.deliverToSultan(playerState);

            assertThat(actionResult.getFollowUpActions())
                    .hasOnlyOneElementSatisfying(possibleAction -> {
                        assertThat(possibleAction.getPossibleActions()).containsExactlyInAnyOrder(
                                Action.Pay1Fabric.class, Action.Pay1Blue.class, Action.Pay1Fruit.class, Action.Pay1Spice.class);

                        possibleAction.perform(Action.Pay1Fabric.class);

                        assertThat(possibleAction.isCompleted()).isTrue();
                    });

            verify(playerState).gainRubies(1);
            assertThat(sultansPalace.getUncovered()).isEqualTo(6);
        }
        // TODO PAY_2_LIRA_TO_RETURN_ASSISTANT gives error
        // TODO both mosque locations active when selecting tiles
        // TODO Als PAY_2_LIRA_TO_RETURN_ASSISTANT nog kan, maar klik skip, dan error NO ACTION
        // TODO Send family member frontend broken
        // TODO PAY_2_LIRA_TO_RETURN_ASSISTANT mosque tiles plaatjes bij player board
        // TODO Test Sultan with any with smuggler
        // TODO Sultan actually pays the goods?
        // TODO Popover hover on player discs and objects
        // TODO "Lift up" discs to reveal whats undernearth
        // TODO Select goods to sell, add button 'all'

        @Test
        void oneAnyGoodNotEnoughGoods() {
            var sultansPalace = Place.SultansPalace.withUncovered(5);

            when(playerState.hasAtLeastGoods(GoodsType.FABRIC, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.SPICE, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.FRUIT, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.BLUE, 1)).thenReturn(true);
            when(playerState.getTotalGoods()).thenReturn(4);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(playerState))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NOT_ENOUGH_GOODS.name());
        }

        @Test
        void twoAnyGoods() {
            var sultansPalace = Place.SultansPalace.withUncovered(10);

            when(playerState.hasAtLeastGoods(GoodsType.FABRIC, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.SPICE, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.FRUIT, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.BLUE, 2)).thenReturn(true);
            when(playerState.getTotalGoods()).thenReturn(10);

            var actionResult = sultansPalace.deliverToSultan(playerState);

            assertThat(actionResult.getFollowUpActions()).hasSize(1);
            var followUpAction = actionResult.getFollowUpActions().get(0);

            assertThat(followUpAction.getPossibleActions()).containsExactlyInAnyOrder(
                    Action.Pay1Fabric.class, Action.Pay1Blue.class, Action.Pay1Fruit.class, Action.Pay1Spice.class);

            followUpAction.perform(Action.Pay1Fabric.class);

            assertThat(followUpAction.getPossibleActions()).containsExactlyInAnyOrder(
                    Action.Pay1Fabric.class, Action.Pay1Blue.class, Action.Pay1Fruit.class, Action.Pay1Spice.class);

            followUpAction.perform(Action.Pay1Fabric.class);

            assertThat(followUpAction.getPossibleActions()).isEmpty();

            verify(playerState).gainRubies(1);
            assertThat(sultansPalace.getUncovered()).isEqualTo(11);
        }

        @Test
        void twoAnyGoodsNotEnoughGoods() {
            var sultansPalace = Place.SultansPalace.withUncovered(10);

            when(playerState.hasAtLeastGoods(GoodsType.FABRIC, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.SPICE, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.FRUIT, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.BLUE, 2)).thenReturn(true);
            when(playerState.getTotalGoods()).thenReturn(9);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(playerState))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NOT_ENOUGH_GOODS.name());
        }

        @Test
        void noRubyAvailable() {
            var sultansPalace = Place.SultansPalace.withUncovered(11);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(playerState))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NO_RUBY_AVAILABLE.name());
        }
    }

    @Nested
    class GemstoneDealer {

        @Mock
        PlayerState playerState;

        @Test
        void forPlayerCount() {
            assertThat(Place.GemstoneDealer.forPlayerCount(2).getCost()).isEqualTo(16);
            assertThat(Place.GemstoneDealer.forPlayerCount(3).getCost()).isEqualTo(15);
            assertThat(Place.GemstoneDealer.forPlayerCount(4).getCost()).isEqualTo(13);
            assertThat(Place.GemstoneDealer.forPlayerCount(5).getCost()).isEqualTo(13);
        }

        @Test
        void buy() {
            var gemstoneDealer = Place.GemstoneDealer.withCost(16);

            gemstoneDealer.buy(playerState);

            verify(playerState).payLira(16);
            verify(playerState).gainRubies(1);

            assertThat(gemstoneDealer.getCost()).isEqualTo(17);
        }

        @Test
        void noRubyAvailable() {
            var gemstoneDealer = Place.GemstoneDealer.withCost(24);

            assertThatThrownBy(() -> gemstoneDealer.buy(playerState))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NO_RUBY_AVAILABLE.name());
        }
    }
}