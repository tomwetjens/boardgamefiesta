package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.istanbul.logic.Place;
import lombok.Getter;

@Getter
public class SultansPalaceView extends PlaceView {

    private final int uncovered;

    SultansPalaceView(Place.SultansPalace sultansPalace) {
        super(sultansPalace);

        this.uncovered = sultansPalace.getUncovered();
    }
}
