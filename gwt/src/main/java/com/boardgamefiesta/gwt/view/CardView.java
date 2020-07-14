package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Card;
import com.boardgamefiesta.gwt.logic.ObjectiveCard;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public abstract class CardView implements Comparable<CardView> {

    public static CardView of(Card card) {
        if (card instanceof Card.CattleCard) {
            return new CattleCardView((Card.CattleCard) card);
        } else {
            return new ObjectiveCardView((ObjectiveCard) card);
        }
    }

    @Override
    public int compareTo(CardView o) {
        if (o instanceof CattleCardView) {
            return 1;
        } else if (o instanceof ObjectiveCardView) {
            return -1;
        } else {
            return 0;
        }
    }
}
