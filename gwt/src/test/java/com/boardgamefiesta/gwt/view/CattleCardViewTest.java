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

import com.boardgamefiesta.gwt.logic.CattleType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CattleCardViewTest {

    @Test
    void compareTo() {
        var list = new ArrayList<>(List.of(
                new CattleCardView(CattleType.TEXAS_LONGHORN, 5, 6),
                new CattleCardView(CattleType.TEXAS_LONGHORN, 5, 5),
                new CattleCardView(CattleType.WEST_HIGHLAND, 4, 4),
                new CattleCardView(CattleType.WEST_HIGHLAND, 4, 3),
                new CattleCardView(CattleType.AYRSHIRE, 3, 3),
                new CattleCardView(CattleType.AYRSHIRE, 3, 3),
                new CattleCardView(CattleType.BROWN_SWISS, 3, 3),
                new CattleCardView(CattleType.BROWN_SWISS, 3, 3),
                new CattleCardView(CattleType.HOLSTEIN, 3, 3),
                new CattleCardView(CattleType.HOLSTEIN, 3, 3),
                new CattleCardView(CattleType.BLACK_ANGUS, 2, 0),
                new CattleCardView(CattleType.GUERNSEY, 2, 0),
                new CattleCardView(CattleType.JERSEY, 1, 0),
                new CattleCardView(CattleType.SIMMENTAL, 5, 5),
                new CattleCardView(CattleType.SIMMENTAL, 3, 4),
                new CattleCardView(CattleType.SIMMENTAL, 2, 2)
        ));

        Collections.sort(list);

        assertThat(list).containsExactly(
                new CattleCardView(CattleType.JERSEY, 1, 0),
                new CattleCardView(CattleType.GUERNSEY, 2, 0),
                new CattleCardView(CattleType.BLACK_ANGUS, 2, 0),
                new CattleCardView(CattleType.SIMMENTAL, 2, 2),
                new CattleCardView(CattleType.SIMMENTAL, 3, 4),
                new CattleCardView(CattleType.SIMMENTAL, 5, 5),
                new CattleCardView(CattleType.HOLSTEIN, 3, 3),
                new CattleCardView(CattleType.HOLSTEIN, 3, 3),
                new CattleCardView(CattleType.BROWN_SWISS, 3, 3),
                new CattleCardView(CattleType.BROWN_SWISS, 3, 3),
                new CattleCardView(CattleType.AYRSHIRE, 3, 3),
                new CattleCardView(CattleType.AYRSHIRE, 3, 3),
                new CattleCardView(CattleType.WEST_HIGHLAND, 4, 3),
                new CattleCardView(CattleType.WEST_HIGHLAND, 4, 4),
                new CattleCardView(CattleType.TEXAS_LONGHORN, 5, 5),
                new CattleCardView(CattleType.TEXAS_LONGHORN, 5, 6)
        );
    }
}