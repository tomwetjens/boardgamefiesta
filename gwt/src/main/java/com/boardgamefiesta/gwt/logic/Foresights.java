package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Foresights {

    private static final int NUM_COLUMNS = 3;
    private static final int NUM_ROWS = 2;

    private final KansasCitySupply kansasCitySupply;
    private final KansasCitySupply.Tile[][] spaces;

    Foresights(KansasCitySupply kansasCitySupply) {
        this(kansasCitySupply, drawInitialTiles(kansasCitySupply));
    }

    private static KansasCitySupply.Tile[][] drawInitialTiles(KansasCitySupply kansasCitySupply) {
        var spaces = new KansasCitySupply.Tile[NUM_COLUMNS][NUM_ROWS];

        for (int columnIndex = 0; columnIndex < NUM_COLUMNS; columnIndex++) {
            for (int rowIndex = 0; rowIndex < NUM_ROWS; rowIndex++) {
                spaces[columnIndex][rowIndex] = kansasCitySupply.draw(columnIndex);
            }
        }

        return spaces;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("spaces", serializer.fromStream(Arrays.stream(spaces),
                        tiles -> serializer.fromStream(Arrays.stream(tiles), KansasCitySupply.Tile::serialize)))
                .build();
    }

    static Foresights deserialize(KansasCitySupply kansasCitySupply, JsonObject jsonObject) {
        return new Foresights(kansasCitySupply,
                jsonObject.getJsonArray("spaces").stream()
                        .map(JsonValue::asJsonArray)
                        .map(JsonArray::stream)
                        .map(tiles -> tiles
                                .map(JsonValue::asJsonObject)
                                .map(KansasCitySupply.Tile::deserialize)
                                .toArray(KansasCitySupply.Tile[]::new))
                        .toArray(KansasCitySupply.Tile[][]::new));
    }

    KansasCitySupply.Tile take(int columnIndex, int rowIndex) {
        KansasCitySupply.Tile tile = spaces[columnIndex][rowIndex];

        KansasCitySupply.Tile replacement = kansasCitySupply.draw(columnIndex);
        spaces[columnIndex][rowIndex] = replacement;

        return tile;
    }

    public List<KansasCitySupply.Tile> choices(int columnIndex) {
        return List.of(spaces[columnIndex]);
    }

}
