package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.GoodsType;
import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class MarketView extends PlaceView {

    private final List<GoodsType> demand;

    MarketView(Place.Market market) {
        super(market);

        this.demand = market.getDemand().entrySet().stream()
                .flatMap(entry -> IntStream.range(0, entry.getValue()).mapToObj(i -> entry.getKey()))
                .sorted()
                .collect(Collectors.toList());
    }

}