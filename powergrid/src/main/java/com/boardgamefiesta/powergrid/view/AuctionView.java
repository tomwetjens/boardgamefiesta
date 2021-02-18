package com.boardgamefiesta.powergrid.view;

import com.boardgamefiesta.powergrid.logic.Auction;
import lombok.Value;

@Value
public class AuctionView {

    PowerPlantView powerPlant;
    Integer bid;

    AuctionView(Auction auction) {
        powerPlant = new PowerPlantView(auction.getPowerPlant());
        bid = auction.getBid().orElse(null);
    }

}
