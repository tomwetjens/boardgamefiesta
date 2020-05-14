package com.wetjens.gwt.view;

import java.util.Comparator;

import com.wetjens.gwt.Card;
import com.wetjens.gwt.CattleType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

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
