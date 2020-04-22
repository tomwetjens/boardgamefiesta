package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.ObjectiveCard;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ObjectiveCardView extends CardView {

    private final int points;
    private final int penalty;
    private final List<ObjectiveCard.Task> tasks;
    private final ActionType action;

    ObjectiveCardView(ObjectiveCard objectiveCard) {
        points = objectiveCard.getPoints();
        penalty = objectiveCard.getPenalty();
        tasks = objectiveCard.getTasks();
        action = objectiveCard.getAction().map(ActionType::of).orElse(null);
    }

}
