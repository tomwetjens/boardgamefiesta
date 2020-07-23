package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.CattleMarket;
import lombok.Value;

@Value
public class PossibleBuyView {

    int breedingValue;
    boolean pair;
    int cost;
    int cowboysNeeded;

    public PossibleBuyView(CattleMarket.PossibleBuy possibleBuy) {
        breedingValue = possibleBuy.getBreedingValue();
        pair = possibleBuy.isPair();
        cost = possibleBuy.getCost();
        cowboysNeeded = possibleBuy.getCowboysNeeded();
    }
}
