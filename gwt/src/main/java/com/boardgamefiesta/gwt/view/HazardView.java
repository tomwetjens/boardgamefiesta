package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Hand;
import com.boardgamefiesta.gwt.logic.Hazard;
import com.boardgamefiesta.gwt.logic.HazardType;
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
