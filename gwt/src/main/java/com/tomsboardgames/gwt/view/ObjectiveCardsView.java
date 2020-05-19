package com.tomsboardgames.gwt.view;

import com.tomsboardgames.gwt.ObjectiveCards;
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
