package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.ObjectiveCard;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ObjectiveCardViewTest {

    @Nested
    class Sorting {

        @Test
        void actionNull() {
            var list = Arrays.asList(
                    new ObjectiveCardView(3, 0, List.of(ObjectiveCard.Task.BUILDING), null),
                    new ObjectiveCardView(3, 2, List.of(ObjectiveCard.Task.BUILDING), ActionType.GAIN_2_DOLLARS));

            Collections.sort(list);

            // TODO assertions
        }
    }

}
