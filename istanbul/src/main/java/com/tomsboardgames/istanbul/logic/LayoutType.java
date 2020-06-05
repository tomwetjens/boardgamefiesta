package com.tomsboardgames.istanbul.logic;

import lombok.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public enum LayoutType {

    SHORT_PATHS() {
        @Override
        Place[][] createLayout(int playerCount, @NonNull Random random) {
            return new Place[][]{
                    {Place.GreatMosque.forPlayerCount(playerCount), new Place.PostOffice(), new Place.FabricWarehouse(), Place.SmallMosque.forPlayerCount(playerCount)},
                    {new Place.FruitWarehouse(), new Place.PoliceStation(), new Place.Fountain(), new Place.SpiceWarehouse()},
                    {new Place.BlackMarket(), new Place.Caravansary(), Place.SmallMarket.randomize(random), new Place.TeaHouse()},
                    {Place.SultansPalace.forPlayerCount(playerCount), Place.LargeMarket.randomize(random), new Place.Wainwright(), Place.GemstoneDealer.forPlayerCount(playerCount)}
            };
        }
    },

    LONG_PATHS() {
        @Override
        Place[][] createLayout(int playerCount, @NonNull Random random) {
            return new Place[][]{
                    {Place.GemstoneDealer.forPlayerCount(playerCount), new Place.FabricWarehouse(), new Place.BlackMarket(), Place.SmallMarket.randomize(random)},
                    {Place.GreatMosque.forPlayerCount(playerCount), new Place.Fountain(), new Place.Caravansary(), new Place.FruitWarehouse()},
                    {new Place.SpiceWarehouse(), new Place.PostOffice(), new Place.PoliceStation(), new Place.Wainwright()},
                    {Place.LargeMarket.randomize(random), new Place.TeaHouse(), Place.SmallMosque.forPlayerCount(playerCount), Place.SultansPalace.forPlayerCount(playerCount)},
            };
        }
    },

    IN_ORDER() {
        @Override
        Place[][] createLayout(int playerCount, @NonNull Random random) {
            return new Place[][]{
                    {new Place.Wainwright(), new Place.FabricWarehouse(), new Place.SpiceWarehouse(), new Place.FruitWarehouse()},
                    {new Place.PostOffice(), new Place.Caravansary(), new Place.Fountain(), new Place.BlackMarket()},
                    {new Place.TeaHouse(), Place.LargeMarket.randomize(random), Place.SmallMarket.randomize(random), new Place.PoliceStation()},
                    {Place.SultansPalace.forPlayerCount(playerCount), Place.SmallMosque.forPlayerCount(playerCount), Place.GreatMosque.forPlayerCount(playerCount), Place.GemstoneDealer.forPlayerCount(playerCount)}
            };
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
