package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.CattleMarket;
import lombok.Value;

@Value
public class PossibleBuyView {

    int breedingValue;
    boolean pair;
    int dollars;
    int cowboys;

    public PossibleBuyView(CattleMarket.PossibleBuy possibleBuy) {
        breedingValue = possibleBuy.getBreedingValue();
        pair = possibleBuy.isPair();
        dollars = possibleBuy.getDollars();
        cowboys = possibleBuy.getCowboys();
    }
}
