package com.boardgamefiesta.gwt.view;

import lombok.Value;

@Value
public class ObjectiveView implements Comparable<ObjectiveView> {

    ObjectiveCardView objectiveCard;
    int score;
    boolean committed;

    ObjectiveView(ObjectiveCardView objectiveCard, int score, boolean committed) {
        this.objectiveCard = objectiveCard;
        this.score = score;
        this.committed = committed;
    }

    @Override
    public int compareTo(ObjectiveView o) {
        return objectiveCard.compareTo(o.objectiveCard);
    }
}
