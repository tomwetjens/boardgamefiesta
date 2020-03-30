package com.wetjens.gwt.server;

import com.wetjens.gwt.Fee;
import com.wetjens.gwt.Hazard;
import com.wetjens.gwt.HazardType;
import lombok.Value;

@Value
public class HazardView {

    private final Fee hands;
    private final int points;
    private final HazardType type;

    HazardView(Hazard hazard) {
        hands = hazard.getFee();
        points = hazard.getPoints();
        type = hazard.getType();
    }

}
