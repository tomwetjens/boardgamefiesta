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

import org.junit.jupiter.api.Test;

import javax.json.Json;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CattleMarketTest {

    @Test
    void fillUpDrawStackEmpty() {
        var lastCattleCard = new Card.CattleCard(CattleType.AYRSHIRE, 3, 3);
        var cattleMarket = new CattleMarket(new LinkedList<>(List.of(lastCattleCard)), new HashSet<>(), false);

        cattleMarket.fillUp(2);

        assertThat(cattleMarket.getMarket()).contains(lastCattleCard);
        assertThat(cattleMarket.getDrawStackSize()).isEqualTo(0);
    }

    @Test
    void drawDrawStackEmpty() {
        var cattleMarket = new CattleMarket(new LinkedList<>(), new HashSet<>(), false);

        cattleMarket.draw();

        assertThat(cattleMarket.getMarket()).isEmpty();
        assertThat(cattleMarket.getDrawStackSize()).isEqualTo(0);
    }

    @Test
    void serializeDrawStackEmpty() {
        var cattleMarket = new CattleMarket(new LinkedList<>(), Set.of(new Card.CattleCard(CattleType.AYRSHIRE, 3, 3)), false);

        var jsonObject = cattleMarket.serialize(Json.createBuilderFactory(Collections.emptyMap()));

        assertThat(jsonObject.getJsonArray("drawStack")).hasSize(0);
        assertThat(jsonObject.getJsonArray("market")).hasSize(1);
    }

    @Test
    void serializeMarketEmpty() {
        var cattleMarket = new CattleMarket(new LinkedList<>(), Collections.emptySet(), false);

        var jsonObject = cattleMarket.serialize(Json.createBuilderFactory(Collections.emptyMap()));

        assertThat(jsonObject.getJsonArray("drawStack")).hasSize(0);
        assertThat(jsonObject.getJsonArray("market")).hasSize(0);
    }

    @Test
    void buy2WestHighlandAsCheapAsPossible() {
        var westHighland1 = new Card.CattleCard(CattleType.WEST_HIGHLAND, 4, 4);
        var westHighland2 = new Card.CattleCard(CattleType.WEST_HIGHLAND, 4, 4);
        var cattleMarket = new CattleMarket(new LinkedList<>(), new HashSet<>(Set.of(
                westHighland1,
                westHighland2
        )), false);

        cattleMarket.buy(westHighland1, westHighland2, 6, 12);
    }

    @Test
    void buySimmentalFor8() {
        var simmental = new Card.CattleCard(CattleType.SIMMENTAL, 3, 2);
        var cattleMarket = new CattleMarket(new LinkedList<>(), new HashSet<>(Set.of(
                simmental
        )), false);

        var cost = cattleMarket.buy(simmental, null, 1, 8);

        assertThat(cost.getCowboys()).isEqualTo(1);
        assertThat(cost.getDollars()).isEqualTo(8);
    }

    @Test
    void buySimmentalFor5() {
        var simmental = new Card.CattleCard(CattleType.SIMMENTAL, 3, 2);
        var cattleMarket = new CattleMarket(new LinkedList<>(), new HashSet<>(Set.of(
                simmental
        )), false);

        var cost = cattleMarket.buy(simmental, null, 2, 5);

        assertThat(cost.getCowboys()).isEqualTo(2);
        assertThat(cost.getDollars()).isEqualTo(5);
    }
}
