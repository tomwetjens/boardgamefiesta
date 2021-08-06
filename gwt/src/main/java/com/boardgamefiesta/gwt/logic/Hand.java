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

public enum Hand {

    NONE(0, 0, 0),
    GREEN(2, 2, 1),
    BLACK(2, 1, 2),
    BOTH(4, 3, 3);

    private final int[] amounts;

    Hand(int... fees) {
        this.amounts = fees;
    }

    public int getFee(int playerCount) {
        return amounts[Math.max(0, Math.min(playerCount - 2, 2))];
    }
}
