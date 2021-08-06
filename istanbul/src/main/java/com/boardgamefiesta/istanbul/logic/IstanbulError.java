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

public enum IstanbulError {
    CANNOT_PERFORM_ACTION,
    CANNOT_SKIP_ACTION,
    NO_ACTION,
    NOT_ENOUGH_GOODS,
    ALREADY_HAS_MOSQUE_TILE,
    MOSQUE_TILE_NOT_AVAILABLE,
    PLACE_NOT_REACHABLE,
    NOT_AT_PLACE,
    NO_ASSISTANTS_AVAILABLE,
    NOT_ENOUGH_LIRA,
    INVALID_GUESS,
    NO_RUBY_AVAILABLE,
    NO_FAMILY_MEMBER_TO_CATCH,
    NO_BONUS_CARD_AVAILABLE,
    DOESNT_HAVE_BONUS_CARD,
    CANNOT_ADD_EXTENSION,
    ALREADY_AT_PLACE,
    CANNOT_REROLL
}
