package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Hand;
import com.boardgamefiesta.gwt.logic.Hazard;
import com.boardgamefiesta.gwt.logic.HazardType;
import lombok.Value;

@Value
public class HazardView implements Comparable<HazardView> {

    Hand hands;
    int points;
    HazardType type;

    HazardView(Hazard hazard) {
        hands = hazard.getHand();
        points = hazard.getPoints();
        type = hazard.getType();
    }

    @Override
    public int compareTo(HazardView o) {
        var result = type.compareTo(o.type);
        if (result == 0) {
            result = Integer.compare(points, o.points);
        }
        if (result == 0) {
            result = hands.compareTo(o.hands);
        }
        return result;
    }
}
