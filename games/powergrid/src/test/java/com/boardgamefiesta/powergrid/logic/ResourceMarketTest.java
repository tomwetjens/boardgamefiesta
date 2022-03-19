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