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

    int points;
    int penalty;
    List<ObjectiveCard.Task> tasks;
    ActionType action;

    ObjectiveCardView(ObjectiveCard objectiveCard) {
        points = objectiveCard.getPoints();
        penalty = objectiveCard.getPenalty();
        tasks = objectiveCard.getTasks();
        action = objectiveCard.getPossibleActions().stream()
                // In case of multiple choices, always -reliably- take the first one
                .min(Comparator.comparing(Class::getSimpleName))
                .map(ActionType::of)
                .orElse(null);
    }

    @Override
    public int compareTo(CardView o) {
        if (o instanceof ObjectiveCardView) {
            return COMPARATOR.compare(this, (ObjectiveCardView) o);
        }
        return super.compareTo(o);
    }
}
