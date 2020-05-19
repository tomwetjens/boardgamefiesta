package com.tomsboardgames.gwt.view;

import com.tomsboardgames.gwt.Hand;
import com.tomsboardgames.gwt.Hazard;
import com.tomsboardgames.gwt.HazardType;
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
