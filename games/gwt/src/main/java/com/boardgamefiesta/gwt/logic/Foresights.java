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

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Foresights {

    private static final int NUM_COLUMNS = 3;
    private static final int NUM_ROWS = 2;

    private final KansasCitySupply.Tile[][] spaces;

    static Foresights initial(KansasCitySupply kansasCitySupply) {
        var foresights = empty();
        foresights.fillUp(kansasCitySupply, true);
        return foresights;
    }

    static Foresights empty() {
        return new Foresights(new KansasCitySupply.Tile[NUM_COLUMNS][NUM_ROWS]);
    }

    void fillUp(KansasCitySupply kansasCitySupply, boolean workers) {
        for (int columnIndex = 0; columnIndex < NUM_COLUMNS; columnIndex++) {
            for (int rowIndex = 0; rowIndex < NUM_ROWS; rowIndex++) {
                if (spaces[columnIndex][rowIndex] == null) {
                    spaces[columnIndex][rowIndex] = kansasCitySupply.draw(columnIndex)
                            .filter(tile -> tile.getWorker() == null || workers)
                            .orElse(null);
                }
            }
        }
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("spaces", serializer.fromStream(Arrays.stream(spaces),
                        tiles -> serializer.fromStream(Arrays.stream(tiles), tile -> tile != null ? tile.serialize(factory) : JsonValue.NULL)))
                .build();
    }

    static Foresights deserialize(JsonObject jsonObject) {
        return new Foresights(Arrays.copyOf(
                jsonObject.getJsonArray("spaces").stream()
                        .map(JsonValue::asJsonArray)
                        .map(JsonArray::stream)
                        .map(tiles -> Arrays.copyOf(tiles
                                .map(jsonValue -> jsonValue.getValueType() != JsonValue.ValueType.NULL
                                        ? KansasCitySupply.Tile.deserialize(jsonValue.asJsonObject())
                                        : null)
                                .toArray(KansasCitySupply.Tile[]::new), 2))
                        .toArray(KansasCitySupply.Tile[][]::new), 3));
    }

    KansasCitySupply.Tile take(int columnIndex, int rowIndex) {
        KansasCitySupply.Tile tile = spaces[columnIndex][rowIndex];

        spaces[columnIndex][rowIndex] = null;

        return tile;
    }

    public List<KansasCitySupply.Tile> choices(int columnIndex) {
        return Collections.unmodifiableList(Arrays.asList(spaces[columnIndex]));
    }

    boolean isEmpty() {
        return isEmpty(0) && isEmpty(1) && isEmpty(2);
    }

    boolean isEmpty(int columnIndex) {
        return spaces[columnIndex][0] == null && spaces[columnIndex][1] == null;
    }

    void removeWorkers() {
        for (int columnIndex = 0; columnIndex < NUM_COLUMNS; columnIndex++) {
            for (int rowIndex = 0; rowIndex < NUM_ROWS; rowIndex++) {
                var tile = spaces[columnIndex][rowIndex];
                if (tile != null && tile.getWorker() != null) {
                    spaces[columnIndex][rowIndex] = null;
                }
            }
        }
    }

    int chooseAnyForesight(int columnIndex, Random random) {
        // TODO Just pick a random tile now
        var index = random.nextInt(2);
        if (spaces[columnIndex][index] != null) {
            return index;
        }
        // Pick the other one (could be empty as well)
        return (index + 1) % 2;
    }

}
