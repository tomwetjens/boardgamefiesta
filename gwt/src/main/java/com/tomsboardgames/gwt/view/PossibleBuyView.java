package com.tomsboardgames.gwt.view;

import com.tomsboardgames.gwt.CattleMarket;
import lombok.Value;

import java.util.List;

@Value
public class PossibleBuyView {

    int cost;
    int cowboysNeeded;
    List<Integer> breedingValues;

    public PossibleBuyView(CattleMarket.PossibleBuy possibleBuy) {
        this.cost = possibleBuy.getCost();
        this.cowboysNeeded = possibleBuy.getCowboysNeeded();
        this.breedingValues = possibleBuy.getBreedingValues();
    }
}
