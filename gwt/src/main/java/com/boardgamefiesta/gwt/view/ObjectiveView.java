package com.boardgamefiesta.gwt.view;

import lombok.Value;

@Value
public class ObjectiveView implements Comparable<ObjectiveView> {

    ObjectiveCardView objectiveCard;
    int score;

    ObjectiveView(ObjectiveCardView objectiveCard, int score) {
        this.objectiveCard = objectiveCard;
        this.score = score;
    }

    @Override
    public int compareTo(ObjectiveView o) {
        return objectiveCard.compareTo(o.objectiveCard);
    }
}
