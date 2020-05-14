package com.wetjens.gwt.view;

import com.wetjens.gwt.Hand;
import com.wetjens.gwt.Hazard;
import com.wetjens.gwt.HazardType;
import lombok.Value;

@Value
public class HazardView {

    Hand hands;
    int points;
    HazardType type;

    HazardView(Hazard hazard) {
        hands = hazard.getHand();
        points = hazard.getPoints();
        type = hazard.getType();
    }

}
