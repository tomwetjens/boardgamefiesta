package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.ObjectiveCards;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
public class ObjectiveCardsView {

    List<ObjectiveCardView> available;
    int drawStackSize;

    ObjectiveCardsView(ObjectiveCards objectiveCards) {
        this.available = objectiveCards.getAvailable().stream()
                .map(ObjectiveCardView::new)
                .sorted()
                .collect(Collectors.toList());
        this.drawStackSize = objectiveCards.getDrawStackSize();
    }

}
