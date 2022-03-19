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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceTest {

    @Mock
    Istanbul game;

    @Nested
    class SultansPalace {

        @Mock
        PlayerState playerState;

        @BeforeEach
        void setUp() {
            lenient().when(game.currentPlayerState()).thenReturn(playerState);
        }

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

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(game))
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

            var actionResult = sultansPalace.deliverToSultan(game);

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

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(game))
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

            var actionResult = sultansPalace.deliverToSultan(game);

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

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(game))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NOT_ENOUGH_GOODS.name());
        }

        @Test
        void noRubyAvailable() {
            var sultansPalace = Place.SultansPalace.withUncovered(11);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(game))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NO_RUBY_AVAILABLE.name());
        }
    }

    @Nested
    class GemstoneDealer {

        @Mock
        Istanbul game;

        @Mock
        PlayerState playerState;

        @BeforeEach
        void setUp() {
            lenient().when(game.currentPlayerState()).thenReturn(playerState);
        }

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

            gemstoneDealer.buy(game);

            verify(playerState).payLira(16);
            verify(playerState).gainRubies(1);

            assertThat(gemstoneDealer.getCost()).isEqualTo(17);
        }

        @Test
        void noRubyAvailable() {
            var gemstoneDealer = Place.GemstoneDealer.withCost(24);

            assertThatThrownBy(() -> gemstoneDealer.buy(game))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NO_RUBY_AVAILABLE.name());
        }
    }
}