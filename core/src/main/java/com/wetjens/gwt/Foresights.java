package com.wetjens.gwt;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public final class Foresights implements Serializable {

    private static final int NUM_COLUMNS = 3;
    private static final int NUM_ROWS = 2;

    private final KansasCitySupply kansasCitySupply;
    private final KansasCitySupply.Tile[][] spaces;

    @SuppressWarnings("unchecked")
    Foresights(KansasCitySupply kansasCitySupply) {
        this.kansasCitySupply = kansasCitySupply;

        spaces = new KansasCitySupply.Tile[NUM_COLUMNS][NUM_ROWS];

        for (int columnIndex = 0 ; columnIndex < NUM_COLUMNS; columnIndex++) {
            for (int rowIndex = 0 ; rowIndex < NUM_ROWS; rowIndex++) {
                spaces[columnIndex][rowIndex] = kansasCitySupply.draw(columnIndex);
            }
        }
    }

    KansasCitySupply.Tile take(int columnIndex, int rowIndex) {
        KansasCitySupply.Tile tile = spaces[columnIndex][rowIndex];

        KansasCitySupply.Tile replacement = kansasCitySupply.draw(columnIndex);
        spaces[columnIndex][rowIndex] = replacement;

        return tile;
    }

    public Collection<KansasCitySupply.Tile> choices(int columnIndex) {
        return Collections.unmodifiableList(Arrays.asList(spaces[columnIndex]));
    }
}
