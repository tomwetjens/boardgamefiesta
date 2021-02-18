package com.boardgamefiesta.powergrid.view;

import com.boardgamefiesta.powergrid.logic.PowerPlant;
import com.boardgamefiesta.powergrid.logic.PowerPlantMarket;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
public class PowerPlantMarketView {

    List<PowerPlantView> actual;
    List<PowerPlantView> future;
    int deckSize;

    public PowerPlantMarketView(PowerPlantMarket powerPlantMarket) {
        actual = powerPlantMarket.getActual().stream().map(PowerPlantView::new).collect(Collectors.toList());
        future = powerPlantMarket.getFuture().stream().map(PowerPlantView::new).collect(Collectors.toList());
        deckSize = powerPlantMarket.getDeckSize();
    }
}
