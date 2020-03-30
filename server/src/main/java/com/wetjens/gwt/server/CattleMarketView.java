package com.wetjens.gwt.server;

import com.wetjens.gwt.CattleMarket;
import lombok.Value;

import java.util.Set;
import java.util.stream.Collectors;

@Value
public class CattleMarketView {

    Set<CattleCardView> cards;

    CattleMarketView(CattleMarket cattleMarket) {
        cards = cattleMarket.getMarket().stream().map(CattleCardView::new).collect(Collectors.toSet());
    }
}
