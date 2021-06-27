package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.CattleMarket;
import com.boardgamefiesta.gwt.logic.GWT;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class CattleMarketView {

    List<CattleCardView> cards;
    int drawStackSize;
    List<CattleCardView> drawStack;

    CattleMarketView(GWT.Options.Mode mode, CattleMarket cattleMarket) {
        cards = cattleMarket.getMarket().stream()
                .map(CattleCardView::new)
                .sorted()
                .collect(Collectors.toList());

        drawStackSize = cattleMarket.getDrawStackSize();

        if (mode == GWT.Options.Mode.STRATEGIC) {
            drawStack = cattleMarket.getCardsInDrawStack().stream()
                    .map(CattleCardView::new)
                    .collect(Collectors.toList());
            Collections.sort(drawStack);
        } else {
            drawStack = null;
        }
    }
}
