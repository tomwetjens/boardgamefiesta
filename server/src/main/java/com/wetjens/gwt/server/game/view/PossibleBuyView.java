package com.wetjens.gwt.server.game.view;

import java.util.List;

import com.wetjens.gwt.CattleMarket;
import lombok.Value;

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
