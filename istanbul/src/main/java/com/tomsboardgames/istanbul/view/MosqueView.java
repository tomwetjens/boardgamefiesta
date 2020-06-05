package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

@Getter
public class MosqueView extends PlaceView {

    private final Integer a;
    private final Integer b;

    MosqueView(Place.Mosque mosque) {
        super(mosque);

        a = mosque.getA().orElse(null);
        b = mosque.getB().orElse(null);
    }

}
