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
