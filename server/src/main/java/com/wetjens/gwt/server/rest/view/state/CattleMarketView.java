package com.wetjens.gwt.server.rest.view.state;

import java.util.List;
import java.util.stream.Collectors;

import com.wetjens.gwt.CattleMarket;
import lombok.Value;

@Value
public class CattleMarketView {

    List<CattleCardView> cards;

    CattleMarketView(CattleMarket cattleMarket) {
        cards = cattleMarket.getMarket().stream()
                .map(CattleCardView::new)
                .sorted()
                .collect(Collectors.toList());
    }
}
