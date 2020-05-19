package com.tomsboardgames.gwt.view;

import com.tomsboardgames.gwt.CattleMarket;
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
