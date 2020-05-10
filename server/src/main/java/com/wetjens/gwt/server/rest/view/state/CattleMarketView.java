package com.wetjens.gwt.server.rest.view.state;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wetjens.gwt.CattleMarket;
import lombok.Value;

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
