package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.BonusCard;
import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

import java.util.List;

@Getter
public class CaravansaryView extends PlaceView {

    private final List<BonusCard> discardPile;

    CaravansaryView(Place.Caravansary caravansary) {
        super(caravansary);

        this.discardPile = caravansary.getDiscardPile();
    }
}
