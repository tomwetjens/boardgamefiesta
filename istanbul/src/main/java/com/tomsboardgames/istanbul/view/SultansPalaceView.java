package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

@Getter
public class SultansPalaceView extends PlaceView {

    private final int uncovered;

    SultansPalaceView(Place.SultansPalace sultansPalace) {
        super(sultansPalace);

        this.uncovered = sultansPalace.getUncovered();
    }
}
