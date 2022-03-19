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

package com.boardgamefiesta.istanbul.logic;

import org.junit.jupiter.api.Test;

import java.util.Random;

class LayoutTypeTest {

    @Test
    void random() {
        var layout = LayoutType.RANDOM.createLayout(2, new Random());

        // TODO Assert The Fountain 7 has to be on one of the 4 Places in the middle of the grid.
        // TODO Assert The Black Market 8 and the Tea House 9 should have a distance from each other of at least 3 Places
    }
}