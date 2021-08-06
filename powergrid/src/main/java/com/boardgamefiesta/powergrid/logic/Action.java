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

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.powergrid.logic.map.City;
import lombok.Value;

import java.util.Map;
import java.util.Random;

public interface Action extends com.boardgamefiesta.api.domain.Action {

    void perform(PowerGrid powerGrid, Player player, Random random);

    @Value
    class ConnectCity implements Action {
        City city;

        @Override
        public void perform(PowerGrid powerGrid, Player player, Random random) {

        }
    }

    @Value
    class PlaceBid implements Action {
        int bid;

        @Override
        public void perform(PowerGrid powerGrid, Player player, Random random) {
            powerGrid.placeBid(bid);
        }
    }

    @Value
    class BuyResource implements Action {

        ResourceType resourceType;
        int amount;

        @Override
        public void perform(PowerGrid powerGrid, Player player, Random random) {
            powerGrid.buyResource(resourceType, amount);
        }
    }

    @Value
    class ProducePower implements Action {
        Map<ResourceType, Integer> resources;

        @Override
        public void perform(PowerGrid powerGrid, Player player, Random random) {

        }
    }

    @Value
    class RemovePowerPlant implements Action {
        PowerPlant powerPlant;

        @Override
        public void perform(PowerGrid powerGrid, Player player, Random random) {

        }
    }

    @Value
    class StartAuction implements Action {
        PowerPlant powerPlant;

        @Override
        public void perform(PowerGrid powerGrid, Player player, Random random) {
            powerGrid.startAuction(powerPlant);
        }
    }
}
