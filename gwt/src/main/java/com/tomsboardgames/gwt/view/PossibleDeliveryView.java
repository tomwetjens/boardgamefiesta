package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.City;
import com.boardgamefiesta.gwt.RailroadTrack;
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
