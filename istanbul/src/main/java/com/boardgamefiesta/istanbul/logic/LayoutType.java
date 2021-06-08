package com.boardgamefiesta.istanbul.logic;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public enum LayoutType {

    SHORT_PATHS() {
        @Override
        Layout createLayout(int playerCount, @NonNull Random random) {
            return new Layout(
                    Place.GreatMosque.forPlayerCount(playerCount), new Place.PostOffice(), new Place.FabricWarehouse(), Place.SmallMosque.forPlayerCount(playerCount),
                    new Place.FruitWarehouse(), new Place.PoliceStation(), new Place.Fountain(), new Place.SpiceWarehouse(),
                    new Place.BlackMarket(), new Place.Caravansary(), Place.SmallMarket.randomize(random), new Place.TeaHouse(),
                    Place.SultansPalace.forPlayerCount(playerCount), Place.LargeMarket.randomize(random), new Place.Wainwright(), Place.GemstoneDealer.forPlayerCount(playerCount)
            );
        }
    },

    LONG_PATHS() {
        @Override
        Layout createLayout(int playerCount, @NonNull Random random) {
            return new Layout(
                    Place.GemstoneDealer.forPlayerCount(playerCount), new Place.FabricWarehouse(), new Place.BlackMarket(), Place.SmallMarket.randomize(random),
                    Place.GreatMosque.forPlayerCount(playerCount), new Place.Fountain(), new Place.Caravansary(), new Place.FruitWarehouse(),
                    new Place.SpiceWarehouse(), new Place.PostOffice(), new Place.PoliceStation(), new Place.Wainwright(),
                    Place.LargeMarket.randomize(random), new Place.TeaHouse(), Place.SmallMosque.forPlayerCount(playerCount), Place.SultansPalace.forPlayerCount(playerCount)
            );
        }
    },

    IN_ORDER() {
        @Override
        Layout createLayout(int playerCount, @NonNull Random random) {
            return new Layout(
                    new Place.Wainwright(), new Place.FabricWarehouse(), new Place.SpiceWarehouse(), new Place.FruitWarehouse(),
                    new Place.PostOffice(), new Place.Caravansary(), new Place.Fountain(), new Place.BlackMarket(),
                    new Place.TeaHouse(), Place.LargeMarket.randomize(random), Place.SmallMarket.randomize(random), new Place.PoliceStation(),
                    Place.SultansPalace.forPlayerCount(playerCount), Place.SmallMosque.forPlayerCount(playerCount), Place.GreatMosque.forPlayerCount(playerCount), Place.GemstoneDealer.forPlayerCount(playerCount)
            );
        }
    },

    RANDOM() {
        private static final int MIN_BLACK_MARKET_TEA_HOUSE_DISTANCE = 3;

        private int dist(int x1, int y1, int x2, int y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }

        @Override
        Layout createLayout(int playerCount, @NonNull Random random) {
            Place[][] layout = new Place[Layout.WIDTH][Layout.HEIGHT];

            // Please note: even with a random layout, it is recommended that you use the following restrictions:
            // The Fountain 7 has to be on one of the 4 Places in the middle of the grid.
            // The Black Market 8 and the Tea House 9 should have a distance from each other of at least 3 Places

            // First place Fountain in the middle of the grid
            layout[random.nextInt(1) + Layout.WIDTH / 2 - 1][random.nextInt(1) + Layout.HEIGHT / 2 - 1] = new Place.Fountain();

            // Then place Black Market anywhere
            int blackMarketX;
            int blackMarketY;
            do {
                blackMarketX = random.nextInt(Layout.WIDTH);
                blackMarketY = random.nextInt(Layout.HEIGHT);
            } while (layout[blackMarketX][blackMarketY] != null);
            layout[blackMarketX][blackMarketY] = new Place.BlackMarket();

            // Then place Tea House at least 3 distance from Black Market
            int teaHouseX;
            int teaHouseY;
            do {
                teaHouseX = random.nextInt(Layout.WIDTH);
                teaHouseY = random.nextInt(Layout.HEIGHT);
            } while (layout[teaHouseX][teaHouseY] != null || dist(blackMarketX, blackMarketY, teaHouseX, teaHouseY) < MIN_BLACK_MARKET_TEA_HOUSE_DISTANCE);
            layout[teaHouseX][teaHouseY] = new Place.TeaHouse();

            // Place the rest randomly
            var places = new ArrayList<>(Arrays.asList(
                    new Place.Caravansary(),
                    new Place.FabricWarehouse(),
                    new Place.FruitWarehouse(),
                    Place.GemstoneDealer.forPlayerCount(playerCount),
                    Place.GreatMosque.forPlayerCount(playerCount),
                    Place.LargeMarket.randomize(random),
                    new Place.PoliceStation(),
                    new Place.PostOffice(),
                    Place.SmallMarket.randomize(random),
                    Place.SmallMosque.forPlayerCount(playerCount),
                    new Place.SpiceWarehouse(),
                    Place.SultansPalace.forPlayerCount(playerCount),
                    new Place.Wainwright()));
            Collections.shuffle(places, random);

            for (var y = 0; y < Layout.HEIGHT; y++) {
                for (var x = 0; x < Layout.WIDTH; x++) {
                    if (layout[x][y] == null) {
                        layout[x][y] = places.remove(0);
                    }
                }
            }

            return new Layout(layout);
        }
    };

    abstract Layout createLayout(int playerCount, @NonNull Random random);
}
