package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.ObjectiveCard;
import lombok.Value;

import java.util.List;

@Value
public class ObjectiveView implements Comparable<ObjectiveView> {

    ObjectiveCardView objectiveCard;
    int score;

    // TODO For backwards compatibility, remove asap
    int points;
    int penalty;
    List<ObjectiveCard.Task> tasks;
    ActionType action;

    ObjectiveView(ObjectiveCardView objectiveCard, int score) {
        this.objectiveCard = objectiveCard;
        this.score = score;

        this.points = objectiveCard.getPoints();
        this.penalty = objectiveCard.getPenalty();
        this.tasks = objectiveCard.getTasks();
        this.action = objectiveCard.getAction();
    }

    @Override
    public int compareTo(ObjectiveView o) {
        return objectiveCard.compareTo(o.objectiveCard);
    }
}
