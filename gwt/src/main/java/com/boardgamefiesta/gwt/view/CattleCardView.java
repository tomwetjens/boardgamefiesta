package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Card;
import com.boardgamefiesta.gwt.logic.CattleType;
import lombok.*;

import java.util.Comparator;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class CattleCardView extends CardView {

    private static final Comparator<CattleCardView> COMPARATOR = Comparator.comparing(CattleCardView::getType)
            .thenComparingInt(CattleCardView::getPoints);

    CattleType type;
    int breedingValue;
    int points;

    CattleCardView(Card.CattleCard cattleCard) {
        type = cattleCard.getType();
        breedingValue = cattleCard.getType().getValue();
        points = cattleCard.getPoints();
    }

    @Override
    public int compareTo(CardView o) {
        if (o instanceof CattleCardView) {
            return COMPARATOR.compare(this, (CattleCardView) o);
        }
        return super.compareTo(o);
    }
}
