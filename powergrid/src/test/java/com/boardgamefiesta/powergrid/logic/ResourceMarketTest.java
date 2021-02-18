package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.powergrid.logic.ResourceMarket;
import com.boardgamefiesta.powergrid.logic.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceMarketTest {

    @Test
    void calculateCost() {
        var resourceMarket = ResourceMarket.create();

        assertEquals(24, resourceMarket.getAvailable(ResourceType.COAL));
        assertEquals(24, resourceMarket.getCapacity(ResourceType.COAL));

        assertEquals(1, resourceMarket.calculateCost(ResourceType.COAL, 1));
        assertEquals(3, resourceMarket.calculateCost(ResourceType.COAL, 3));
        assertEquals(5, resourceMarket.calculateCost(ResourceType.COAL, 4));
        assertEquals(9, resourceMarket.calculateCost(ResourceType.COAL, 6));
        assertEquals(108, resourceMarket.calculateCost(ResourceType.COAL, 24));
    }

    @Test
    void calculateCostUranium() {
        var resourceMarket = new ResourceMarket(Map.of(ResourceType.URANIUM, 12));

        assertEquals(12, resourceMarket.getAvailable(ResourceType.URANIUM));
        assertEquals(12, resourceMarket.getCapacity(ResourceType.URANIUM));

        assertEquals(1, resourceMarket.calculateCost(ResourceType.URANIUM, 1));
        assertEquals(3, resourceMarket.calculateCost(ResourceType.URANIUM, 2));
        assertEquals(10, resourceMarket.calculateCost(ResourceType.URANIUM, 4));
        assertEquals(36, resourceMarket.calculateCost(ResourceType.URANIUM, 8));
        assertEquals(46, resourceMarket.calculateCost(ResourceType.URANIUM, 9));
        assertEquals(88, resourceMarket.calculateCost(ResourceType.URANIUM, 12));
    }

}