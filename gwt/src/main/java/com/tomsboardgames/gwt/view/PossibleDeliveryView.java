package com.tomsboardgames.gwt.view;

import com.tomsboardgames.gwt.City;
import com.tomsboardgames.gwt.RailroadTrack;
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
