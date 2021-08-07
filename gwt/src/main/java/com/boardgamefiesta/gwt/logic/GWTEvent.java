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

import com.boardgamefiesta.api.domain.InGameEvent;
import com.boardgamefiesta.api.domain.Player;
import lombok.Value;

import java.util.List;

@Value
public class GWTEvent implements InGameEvent {

    Player player;
    String type;
    List<String> parameters;

    GWTEvent(Player player, Type type, List<String> parameters) {
        this.player = player;
        this.type = type.name();
        this.parameters = parameters;
    }

    public enum Type {
        ACTION,
        PAY_FEE_PLAYER,
        PAY_FEE_BANK,
        MAY_APPOINT_STATION_MASTER,
        MUST_TAKE_OBJECTIVE_CARD,
        MAY_REMOVE_BLACK_DISC_INSTEAD_OF_WHITE,
        MUST_REMOVE_DISC_FROM_STATION,
        MAY_REMOVE_HAZARD_FOR_FREE,
        MAY_TRADE_WITH_TRIBES,
        MAY_PLACE_CHEAP_BUILDING,
        MAY_DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE,
        MAY_DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS,
        MAY_HIRE_CHEAP_WORKER,
        MAY_DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES,
        GAINS_JOB_MARKET_TOKEN,
        EVERY_OTHER_PLAYER_HAS_1_TURN,
        FILL_UP_CATTLE_MARKET,
        PLAYER_ORDER,
        MAY_DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS,
        BUY_2_CATTLE,
        BUY_CATTLE,
        MOVE,
        MOVE_WITHOUT_FEES,
        MOVE_TO_BUILDING,
        MOVE_TO_BUILDING_WITHOUT_FEES,
        MOVE_TO_PLAYER_BUILDING,
        MOVE_TO_PLAYER_BUILDING_WITHOUT_FEES,
        DISCARD_CATTLE_CARD,
        DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND,
        ADD_1_OBJECTIVE_CARD_TO_HAND,
        DISCARD_1_CATTLE_CARD_TO_GAIN_1_CERTIFICATE,
        DISCARD_PAIR_TO_GAIN_3_DOLLARS,
        DISCARD_PAIR_TO_GAIN_4_DOLLARS,
        MOVE_ENGINE_1_FORWARD,
        MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS,
        MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD,
        MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS,
        MOVE_ENGINE_2_OR_3_FORWARD,
        MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS,
        MOVE_ENGINE_AT_MOST_2_FORWARD,
        MOVE_ENGINE_AT_MOST_3_FORWARD,
        MOVE_ENGINE_AT_MOST_4_FORWARD,
        MOVE_ENGINE_FORWARD,
        MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS,
        MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_HAZARDS,
        PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE,
        PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD,
        PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES,
        PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD,
        PLAY_OBJECTIVE_CARD,
        REMOVE_OBJECTIVE_CARD,
        REMOVE_CATTLE_CARD,
        TAKE_OBJECTIVE_CARD,
        UPGRADE_ANY_STATION_BEHIND_ENGINE,
        UPGRADE_STATION,
        USE_ADJACENT_BUILDING,
        USE_ADJACENT_PLAYER_BUILDING,
        DOWNGRADE_STATION,
        DISCARD_OBJECTIVE_CARD,
        DISCARD_CATTLE_CARD_TO_GAIN_7_DOLLARS,
        MOVE_ENGINE_2_FORWARD,
        DISCARD_1_CATTLE_CARD_TO_GAIN_6_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND,
        MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD_AND_GAIN_1_DOLLAR,
        MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS_AND_GAIN_2_DOLLARS,
        REMOVE_CATTLE_CARD_AND_GAIN_1_DOLLAR,
        REMOVE_OBJECTIVE_CARD_AND_GAIN_2_DOLLARS,
        DISCARD_1_DUTCH_BELT_TO_MOVE_ENGINE_2_FORWARD
    }
}
