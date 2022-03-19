/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Action;
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
                .min(Comparator.<Class<? extends Action>>comparingInt(c -> c.getSimpleName().length())
                        .thenComparing(Class::getSimpleName))
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
