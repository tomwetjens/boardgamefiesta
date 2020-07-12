package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.istanbul.logic.Place;
import lombok.Getter;

@Getter
public class GemstoneDealerView extends PlaceView {

    private final int cost;

    GemstoneDealerView(Place.GemstoneDealer gemstoneDealer) {
        super(gemstoneDealer);

        this.cost = gemstoneDealer.getCost();
    }
}
