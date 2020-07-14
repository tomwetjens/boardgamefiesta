package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.CattleMarket;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
public class CattleMarketView {

    List<CattleCardView> cards;
    int drawStackSize;

    CattleMarketView(CattleMarket cattleMarket) {
        cards = cattleMarket.getMarket().stream()
                .map(CattleCardView::new)
                .sorted()
                .collect(Collectors.toList());
        drawStackSize = cattleMarket.getDrawStackSize();
    }
}
