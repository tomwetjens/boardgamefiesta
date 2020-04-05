package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.Card;
import com.wetjens.gwt.CattleType;
import lombok.Getter;

@Getter
public class CattleCardView extends CardView {

    CattleType type;
    int breedingValue;
    int points;

    CattleCardView(Card.CattleCard cattleCard) {
        type = cattleCard.getType();
        breedingValue = cattleCard.getType().getValue();
        points = cattleCard.getPoints();
    }

}
