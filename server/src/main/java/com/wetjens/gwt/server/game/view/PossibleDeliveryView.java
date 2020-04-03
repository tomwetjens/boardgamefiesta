package com.wetjens.gwt.server.game.view;

import com.wetjens.gwt.City;
import com.wetjens.gwt.RailroadTrack;
import lombok.Value;

@Value
public class PossibleDeliveryView {

    City city;
    int certificates;

    public PossibleDeliveryView(RailroadTrack.PossibleDelivery possibleDelivery) {
        this.city = possibleDelivery.getCity();
        this.certificates = possibleDelivery.getCertificates();
    }
}
