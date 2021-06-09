package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.istanbul.logic.BonusCard;
import com.boardgamefiesta.istanbul.logic.Place;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class CaravansaryView extends PlaceView {

    private final List<BonusCard> discardPile;

    CaravansaryView(Place.Caravansary caravansary) {
        super(caravansary);

        this.discardPile = caravansary.getDiscardPile();
        Collections.reverse(discardPile);
    }
}
