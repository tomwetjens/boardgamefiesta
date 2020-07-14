package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.ObjectiveCard;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Comparator;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ObjectiveCardView extends CardView {

    private static final Comparator<ObjectiveCardView> COMPARATOR = Comparator.comparing(ObjectiveCardView::getAction, Comparator.nullsFirst(Comparator.naturalOrder()))
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
