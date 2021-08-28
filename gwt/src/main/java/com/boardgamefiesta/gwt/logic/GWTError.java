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

public enum GWTError {
    CANNOT_SKIP_ACTION,
    CANNOT_PERFORM_ACTION,
    ALREADY_AT_SPACE,
    ALREADY_DELIVERED_TO_CITY,
    ALREADY_HAS_HAZARD,
    ALREADY_HAS_STATION_MASTER,
    ALREADY_PLAYER_ON_SPACE,
    ALREADY_UNLOCKED,
    ALREADY_UPGRADED_STATION,
    AT_LEAST_2_PLAYERS_REQUIRED,
    AT_MOST_4_PLAYERS_SUPPORTED,
    BUILDING_NOT_AVAILABLE,
    CANNOT_REPLACE_BUILDING_OF_OTHER_PLAYER,
    CANNOT_REPLACE_NEUTRAL_BUILDING,
    CANNOT_STEP_DIRECTLY_FROM_TO,
    CARD_NOT_IN_HAND,
    CATTLE_CARDS_NOT_IN_HAND, // type, amount
    CATTLE_CARD_NOT_AVAILABLE,
    CITY_VALUE_MUST_BE_LESS_THEN_OR_EQUAL_TO_SPACES_THAT_ENGINE_MOVED_BACKWARDS,
    GAME_ENDED,
    GAME_NOT_ENDED,
    HAZARD_MUST_BE_OF_TYPE,
    HAZARD_NOT_ON_TRAIL,
    JOB_MARKET_CLOSED,
    LOCATION_EMPTY,
    LOCATION_NOT_EMPTY,
    MUST_CHOOSE_ACTION,
    MUST_MOVE_AT_LEAST_STEPS, // atLeast
    MUST_PICK_WHITE_DISC,
    MUST_SPECIFY_3_FORESIGHTS,
    NOT_PAIR,
    NOT_AT_LOCATION,
    NOT_AT_STATION,
    NOT_ENOUGH_BALANCE_TO_PAY, // amount
    NOT_ENOUGH_BREEDING_VALUE, // actual, needed
    NOT_ENOUGH_CATTLE_CARDS_OF_BREEDING_VALUE_AVAILABLE, // breedingValue
    NOT_ENOUGH_CERTIFICATES,
    NOT_ENOUGH_COWBOYS,
    NOT_ENOUGH_CRAFTSMEN,
    NOT_ENOUGH_WORKERS, // worker
    NOT_FIRST_ACTION,
    NO_ACTIONS,
    NO_SUCH_LOCATION,
    NO_SUCH_SPACE,
    NO_TEEPEE_AT_LOCATION,
    OBJECTIVE_CARD_NOT_AVAILABLE,
    REPLACEMENT_BUILDING_MUST_BE_HIGHER,
    REPLACEMENT_BUILDING_MUST_BE_PLAYER_BUILDING,
    SPACE_NOT_REACHABLE, // atLeast, atMost
    STATION_MUST_BE_BEHIND_ENGINE,
    STATION_NOT_UPGRADED_BY_PLAYER,
    STEPS_EXCEED_LIMIT, // stepLimit
    WORKERS_EXCEED_LIMIT,
    NO_SUCH_PLAYER,
    LOCATION_NOT_ADJACENT,
    STATION_NOT_ON_TRACK,
    MUST_START_ON_NEUTRAL_BUILDING,
    WORKER_NOT_AVAILABLE, // worker
    BID_TOO_LOW,
    BID_INVALID_POSITION,
    CITY_NOT_ACCESSIBLE,
    ALREADY_PLACED_BRANCHLET,
    NOT_ENOUGH_EXCHANGE_TOKENS,
    NO_BRANCHLETS,
    NO_SUCH_TOWN,
    STATION_MASTER_NOT_AVAILABLE,
    TOWN_NOT_ACCESSIBLE,
    NOT_CURRENT_PLAYER,
    INVALID_CATTLE_TYPE,
    NO_TILES_LEFT,
    CANNOT_FORCE_END_TURN,
    CANNOT_UPGRADE_SIMMENTAL
}
