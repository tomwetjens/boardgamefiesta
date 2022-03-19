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

import com.boardgamefiesta.api.domain.InGameEvent;
import com.boardgamefiesta.api.domain.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IstanbulEvent implements InGameEvent {

    Player player;
    String type;
    List<String> parameters;

    static IstanbulEvent create(Player player, Type type, int... values) {
        return new IstanbulEvent(player, type.name(), Arrays.stream(values)
                .mapToObj(Integer::toString)
                .collect(Collectors.toUnmodifiableList()));
    }

    static IstanbulEvent create(Player player, Type type) {
        return new IstanbulEvent(player, type.name(), Collections.emptyList());
    }

    static IstanbulEvent create(Player player, Type type, String... values) {
        return new IstanbulEvent(player, type.name(), Arrays.asList(values));
    }

    public enum Type {
        ROLLED,
        TURNED_DIE,
        GUESSED,
        MOVE,
        PLAY_BONUS_CARD,
        LEAVE_ASSISTANT,
        TAKE_GOODS,
        GAIN_LIRA,
        PICK_UP_ASSISTANT,
        FAMILY_MEMBER_TO_POLICE_STATION,
        TAKE_BONUS_CARD,
        PAY_GOODS,
        MUST_PAY_OTHER_MERCHANTS,
        PAY_OTHER_PLAYER,
        PAY_LIRA,
        MOVE_DUMMY,
        USE_GOVERNOR,
        MOVE_GOVERNOR,
        USE_SMUGGLER,
        MOVE_SMUGGLER,
        TAKE_MOSQUE_TILE,
        BUY_WHEELBARROW_EXTENSION,
        MAX_GOODS,
        MAY_PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD,
        PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD,
        PAY_2_LIRA_TO_RETURN_ASSISTANT,
        MAY_USE_GEMSTONE_DEALER_2X,
        MAY_USE_POSTOFFICE_2X,
        MAY_CATCH_FAMILY_MEMBER,
        CATCH_FAMILY_MEMBER,
        DISCARD_BONUS_CARD,
        RETURN_ALL_ASSISTANTS,
        SELL_GOODS,
        SEND_FAMILY_MEMBER,
        MAY_DELIVER_TO_SULTAN_2X,
        BUY_RUBY,
        ROLL_FOR_BLUE,
        LAST_ROUND,
        GAIN_BONUS_CARD,
        GAIN_RUBY_FROM_MOSQUE,
        PLAY_LEFTOVER_BONUS_CARDS,
        MAY_TURN_OR_REROLL_DICE,
        REROLLED,
        GAIN_RUBY_FROM_SULTAN,
        DELIVER_TO_SULTAN
    }
}
