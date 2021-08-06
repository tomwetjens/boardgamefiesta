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

package com.boardgamefiesta.powergrid.logic;

public enum PowerGridError {
    NOT_ENOUGH_AVAILABLE, NOT_AUCTION_PHASE, AUCTION_IN_PROGRESS, BALANCE_TOO_LOW, MUST_START_AUCTION, NO_AUCTION_IN_PROGRESS, BID_TOO_LOW, NOT_BUREAUCRACY_PHASE, ALREADY_PRODUCED_THIS_ROUND, NOT_ENOUGH_PLAYERS, TOO_MANY_PLAYERS, INVALID_NUMBER_OF_AREAS, INVALID_ACTION, NOT_BIDDING_PLAYER, NOT_PLAYERS_TURN, POWER_PLANT_NOT_AVAILABLE
}
