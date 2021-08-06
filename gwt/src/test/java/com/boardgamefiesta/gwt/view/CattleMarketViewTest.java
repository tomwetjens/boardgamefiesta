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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CattleMarketViewTest {

    @Nested
    class Ordering {

        @Test
        void brownSwissBeforeAyshire() {
            var ayshire = CattleCardView.builder()
                    .type(CattleType.AYRSHIRE)
                    .breedingValue(3)
                    .points(3)
                    .build();
            var brownSwiss = CattleCardView.builder()
                    .type(CattleType.BROWN_SWISS)
                    .breedingValue(3)
                    .points(2)
                    .build();

            var list = Arrays.asList(ayshire, brownSwiss);
            Collections.sort(list);

            assertThat(list).containsExactly(brownSwiss, ayshire);
        }
    }

}
