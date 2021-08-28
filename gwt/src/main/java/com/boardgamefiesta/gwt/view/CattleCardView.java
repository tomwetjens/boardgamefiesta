/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
        breedingValue = cattleCard.getValue();
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
