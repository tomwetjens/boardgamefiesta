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

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Unlockable {

    CERT_LIMIT_4(1, DiscColor.WHITE, 0),
    CERT_LIMIT_6(1, DiscColor.BLACK, 0),
    EXTRA_STEP_DOLLARS(1, DiscColor.BLACK, 0),
    EXTRA_STEP_POINTS(1, DiscColor.BLACK, 0),
    EXTRA_CARD(2, DiscColor.BLACK, 5),
    AUX_GAIN_DOLLAR(2, DiscColor.WHITE, 0),
    AUX_DRAW_CARD_TO_DISCARD_CARD(2, DiscColor.WHITE, 0),
    AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT(2, DiscColor.WHITE, 0),
    AUX_PAY_TO_MOVE_ENGINE_FORWARD(2, DiscColor.WHITE, 0),
    AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD(2, DiscColor.WHITE, 0),
    AUX_DISCARD_CATTLE_CARD_TO_PLACE_BRANCHLET(2, DiscColor.WHITE, 2);

    private final int count;
    private final DiscColor discColor;
    private final int cost;

}
