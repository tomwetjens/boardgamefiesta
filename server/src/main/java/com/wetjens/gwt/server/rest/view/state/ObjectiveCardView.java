package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.ObjectiveCard;
import lombok.Value;

import java.util.List;

@Value
public class ObjectiveCardView extends CardView {

    private final int points;
    private final int penalty;
    private final List<ObjectiveCard.Task> tasks;

    ObjectiveCardView(ObjectiveCard objectiveCard) {
        points = objectiveCard.getPoints();
        penalty = objectiveCard.getPenalty();
        tasks = objectiveCard.getTasks();
    }

}
