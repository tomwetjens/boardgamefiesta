package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.Hand;
import com.boardgamefiesta.gwt.Hazard;
import com.boardgamefiesta.gwt.HazardType;
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
