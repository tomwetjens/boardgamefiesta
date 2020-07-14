package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.City;
import com.boardgamefiesta.gwt.logic.RailroadTrack;
import lombok.Value;

@Value
public class PossibleDeliveryView {

    City city;
    int certificates;
    int reward;

    public PossibleDeliveryView(RailroadTrack.PossibleDelivery possibleDelivery) {
        this.city = possibleDelivery.getCity();
        this.certificates = possibleDelivery.getCertificates();
        this.reward = possibleDelivery.getReward();
    }
}
