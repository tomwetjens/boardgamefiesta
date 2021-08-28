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

package com.boardgamefiesta.gwt.logic;

import lombok.Getter;

public enum CattleType {

    JERSEY(1),
    GUERNSEY(2),
    BLACK_ANGUS(2),
    DUTCH_BELT(2),
    HOLSTEIN(3),
    BROWN_SWISS(3),
    AYRSHIRE(3),
    WEST_HIGHLAND(4),
    TEXAS_LONGHORN(5),
    SIMMENTAL(0/*not used*/);

    @Getter
    private final int defaultValue;

    CattleType(int defaultValue) {
        this.defaultValue = defaultValue;
    }
}
