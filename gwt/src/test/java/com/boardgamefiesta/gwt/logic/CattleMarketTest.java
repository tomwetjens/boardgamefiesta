package com.boardgamefiesta.gwt.logic;

import org.junit.jupiter.api.Test;

import javax.json.Json;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CattleMarketTest {

    @Test
    void fillUpDrawStackEmpty() {
        var lastCattleCard = new Card.CattleCard(CattleType.AYRSHIRE, 3);
        var cattleMarket = new CattleMarket(2, new LinkedList<>(List.of(lastCattleCard)), new HashSet<>());

        cattleMarket.fillUp();

        assertThat(cattleMarket.getMarket()).contains(lastCattleCard);
        assertThat(cattleMarket.getDrawStackSize()).isEqualTo(0);
    }

    @Test
    void drawDrawStackEmpty() {
        var cattleMarket = new CattleMarket(2, new LinkedList<>(), new HashSet<>());

        cattleMarket.draw();

        assertThat(cattleMarket.getMarket()).isEmpty();
        assertThat(cattleMarket.getDrawStackSize()).isEqualTo(0);
    }

    @Test
    void serializeDrawStackEmpty() {
        var cattleMarket = new CattleMarket(2, new LinkedList<>(), Set.of(new Card.CattleCard(CattleType.AYRSHIRE, 3)));

        var jsonObject = cattleMarket.serialize(Json.createBuilderFactory(Collections.emptyMap()));

        assertThat(jsonObject.getJsonArray("drawStack")).hasSize(0);
        assertThat(jsonObject.getJsonArray("market")).hasSize(1);
    }

    @Test
    void serializeMarketEmpty() {
        var cattleMarket = new CattleMarket(2, new LinkedList<>(), Collections.emptySet());

        var jsonObject = cattleMarket.serialize(Json.createBuilderFactory(Collections.emptyMap()));

        assertThat(jsonObject.getJsonArray("drawStack")).hasSize(0);
        assertThat(jsonObject.getJsonArray("market")).hasSize(0);
    }


}
