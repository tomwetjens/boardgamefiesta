package com.tomsboardgames.istanbul.logic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

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
        void mustSpecifyPreferredGoodsType() {
            var sultansPalace = Place.SultansPalace.withUncovered(5);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(playerState, Collections.emptySet()))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.MUST_SPECIFY_GOODS_TYPE.name());
        }

        @Test
        void notEnoughGoods() {
            var sultansPalace = Place.SultansPalace.withUncovered(5);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(playerState, Set.of(GoodsType.BLUE)))
                    .isInstanceOf(IstanbulException.class)
                    .hasMessage(IstanbulError.NOT_ENOUGH_GOODS.name());
        }

        @Test
        void oneAnyGood() {
            var sultansPalace = Place.SultansPalace.withUncovered(5);

            when(playerState.hasAtLeastGoods(GoodsType.FABRIC, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.SPICE, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.FRUIT, 1)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.BLUE, 2)).thenReturn(true);

            sultansPalace.deliverToSultan(playerState, Set.of(GoodsType.BLUE));

            verify(playerState).gainRubies(1);
            assertThat(sultansPalace.getUncovered()).isEqualTo(6);
        }

        @Test
        void twoAnyGoodsOneSpecified() {
            var sultansPalace = Place.SultansPalace.withUncovered(10);

            when(playerState.hasAtLeastGoods(GoodsType.FABRIC, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.SPICE, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.FRUIT, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.BLUE, 4)).thenReturn(true);

            sultansPalace.deliverToSultan(playerState, Set.of(GoodsType.BLUE));

            verify(playerState).gainRubies(1);
            assertThat(sultansPalace.getUncovered()).isEqualTo(11);
        }

        @Test
        void twoAnyGoodsTwoSpecified() {
            var sultansPalace = Place.SultansPalace.withUncovered(10);

            when(playerState.hasAtLeastGoods(GoodsType.FABRIC, 3)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.SPICE, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.FRUIT, 2)).thenReturn(true);
            when(playerState.hasAtLeastGoods(GoodsType.BLUE, 3)).thenReturn(true);

            sultansPalace.deliverToSultan(playerState, Set.of(GoodsType.BLUE, GoodsType.FABRIC));

            verify(playerState).gainRubies(1);
            assertThat(sultansPalace.getUncovered()).isEqualTo(11);
        }

        @Test
        void noRubyAvailable() {
            var sultansPalace = Place.SultansPalace.withUncovered(11);

            assertThatThrownBy(() -> sultansPalace.deliverToSultan(playerState, Set.of(GoodsType.BLUE, GoodsType.FABRIC)))
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