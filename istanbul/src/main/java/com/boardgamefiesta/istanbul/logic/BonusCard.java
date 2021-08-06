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

import java.io.DataInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum BonusCard {
    GAIN_1_GOOD,
    TAKE_5_LIRA,
    SULTAN_2X,
    POST_OFFICE_2X,
    GEMSTONE_DEALER_2X,
    FAMILY_MEMBER_TO_POLICE_STATION,
    MOVE_0,
    MOVE_3_OR_4,
    RETURN_1_ASSISTANT,
    SMALL_MARKET_ANY_GOOD;

    static Collection<BonusCard> createDeck() {
        return Stream.of(
                IntStream.range(0, 4).mapToObj(i -> GAIN_1_GOOD),
                IntStream.range(0, 4).mapToObj(i -> TAKE_5_LIRA),
                IntStream.range(0, 2).mapToObj(i -> SULTAN_2X),
                IntStream.range(0, 2).mapToObj(i -> POST_OFFICE_2X),
                IntStream.range(0, 2).mapToObj(i -> GEMSTONE_DEALER_2X),
                IntStream.range(0, 2).mapToObj(i -> FAMILY_MEMBER_TO_POLICE_STATION),
                IntStream.range(0, 2).mapToObj(i -> MOVE_0),
                IntStream.range(0, 4).mapToObj(i -> MOVE_3_OR_4),
                IntStream.range(0, 2).mapToObj(i -> RETURN_1_ASSISTANT),
                IntStream.range(0, 2).mapToObj(i -> SMALL_MARKET_ANY_GOOD))
                .flatMap(Function.identity())
                .collect(Collectors.toList());

    }

}
