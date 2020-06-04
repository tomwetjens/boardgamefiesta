package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

@Getter
public class GemstoneDealerView extends PlaceView {

    private final int cost;

    GemstoneDealerView(Place.GemstoneDealer gemstoneDealer) {
        super(gemstoneDealer);

        this.cost = gemstoneDealer.getCost();
    }
}
