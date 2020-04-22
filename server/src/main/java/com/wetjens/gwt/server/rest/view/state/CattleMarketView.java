package com.wetjens.gwt.server.rest.view.state;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.wetjens.gwt.Card;
import com.wetjens.gwt.CattleMarket;
import lombok.Value;

@Value
public class CattleMarketView {

    List<CattleCardView> cards;

    CattleMarketView(CattleMarket cattleMarket) {
        cards = cattleMarket.getMarket().stream()
                .sorted(Comparator.<Card.CattleCard>comparingInt(card -> card.getType().getValue())
                        .thenComparingInt(Card.CattleCard::getPoints))
                .map(CattleCardView::new)
                .collect(Collectors.toList());
    }
}
