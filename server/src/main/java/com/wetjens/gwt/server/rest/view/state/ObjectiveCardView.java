package com.wetjens.gwt.server.rest.view.state;

import java.util.Comparator;
import java.util.List;

import com.wetjens.gwt.ObjectiveCard;
import com.wetjens.gwt.server.domain.ActionType;
import com.wetjens.gwt.server.rest.view.IterableComparator;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class ObjectiveCardView extends CardView {

    private static final Comparator<ObjectiveCardView> COMPARATOR = Comparator.comparing(ObjectiveCardView::getAction)
            .thenComparingInt(ObjectiveCardView::getPoints)
            .thenComparing(ObjectiveCardView::getTasks, new IterableComparator<>(Comparator.nullsLast(Comparator.naturalOrder())));

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

    @Override
    public int compareTo(CardView o) {
        if (o instanceof ObjectiveCardView) {
            return COMPARATOR.compare(this, (ObjectiveCardView) o);
        }
        return super.compareTo(o);
    }
}
