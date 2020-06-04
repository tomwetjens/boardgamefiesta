package com.tomsboardgames.istanbul.logic;

import lombok.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public enum LayoutType {

    SHORT_PATHS() {
        @Override
        Place[][] createLayout(int playerCount, @NonNull Random random) {
            // TODO
            return new Place[0][];
        }
    },

    LONG_PATHS() {
        @Override
        Place[][] createLayout(int playerCount, @NonNull Random random) {
            // TODO
            return new Place[0][];
        }
    },

    IN_ORDER() {
        @Override
        Place[][] createLayout(int playerCount, @NonNull Random random) {
            // TODO
            return new Place[0][];
        }
    },

    RANDOM() {
        @Override
        Place[][] createLayout(int playerCount, @NonNull Random random) {
            var places = Arrays.asList(
                    new Place.BlackMarket(),
                    new Place.Caravansary(),
                    new Place.FabricWarehouse(),
                    new Place.Fountain(),
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
                    new Place.TeaHouse(),
                    new Place.Wainwright());
            Collections.shuffle(places, random);

            Place[][] layout = new Place[4][4];
            for (var x = 0; x < 4; x++) {
                for (var y = 0; y < 4; y++) {
                    var place = places.get(x * 4 + y);
                    layout[x][y] = place;
                }
            }

            return layout;
        }
    };

    abstract Place[][] createLayout(int playerCount, @NonNull Random random);
}
