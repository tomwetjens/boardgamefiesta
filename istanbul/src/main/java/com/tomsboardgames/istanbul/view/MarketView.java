package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.GoodsType;
import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

import java.util.Map;

@Getter
public class MarketView extends PlaceView {

    private final Map<GoodsType, Integer> demand;

    MarketView(Place.Market market) {
        super(market);

        this.demand = market.getDemand();
    }

}