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
import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Foresights {

    private static final int NUM_COLUMNS = 3;
    private static final int NUM_ROWS = 2;

    private final KansasCitySupply kansasCitySupply;
    private final KansasCitySupply.Tile[][] spaces;

    Foresights(KansasCitySupply kansasCitySupply) {
        this(kansasCitySupply, new KansasCitySupply.Tile[NUM_COLUMNS][NUM_ROWS]);
        fillUp();
    }

    void fillUp() {
        for (int columnIndex = 0; columnIndex < NUM_COLUMNS; columnIndex++) {
            for (int rowIndex = 0; rowIndex < NUM_ROWS; rowIndex++) {
                if (spaces[columnIndex][rowIndex] == null) {
                    spaces[columnIndex][rowIndex] = kansasCitySupply.draw(columnIndex);
                }
            }
        }
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("spaces", serializer.fromStream(Arrays.stream(spaces),
                        tiles -> serializer.fromStream(Arrays.stream(tiles).filter(Objects::nonNull), KansasCitySupply.Tile::serialize)))
                .build();
    }

    static Foresights deserialize(KansasCitySupply kansasCitySupply, JsonObject jsonObject) {
        return new Foresights(kansasCitySupply, Arrays.copyOf(
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
        return spaces[0][0] == null && spaces[0][1] == null
                && spaces[1][0] == null && spaces[1][1] == null
                && spaces[2][0] == null && spaces[2][1] == null;
    }
}
