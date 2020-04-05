package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.Card;
import com.wetjens.gwt.ObjectiveCard;

public abstract class CardView {

    public static CardView of(Card card) {
        if (card instanceof Card.CattleCard) {
            return new CattleCardView((Card.CattleCard) card);
        } else if (card instanceof ObjectiveCard) {
            return new ObjectiveCardView((ObjectiveCard) card);
        } else {
            throw new IllegalArgumentException("Unsupported card: " + card);
        }
    }
}
