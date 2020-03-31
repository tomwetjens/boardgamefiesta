package com.wetjens.gwt.server;

import com.wetjens.gwt.Hand;
import com.wetjens.gwt.Hazard;
import com.wetjens.gwt.HazardType;
import lombok.Value;

@Value
public class HazardView {

    private final Hand hands;
    private final int points;
    private final HazardType type;

    HazardView(Hazard hazard) {
        hands = hazard.getHand();
        points = hazard.getPoints();
        type = hazard.getType();
    }

}
