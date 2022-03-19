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
