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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum MediumTownTile {

    GAIN_5_DOLLARS_OR_TAKE_CATTLE_CARD(PossibleAction.choice(
            Action.Gain5Dollars.class,
            Action.TakeBreedingValue3CattleCard.class)),
    HIRE_WORKER_PLUS_2(PossibleAction.optional(Action.HireWorkerMinus2.class)),
    REMOVE_2_CARDS(PossibleAction.repeat(0, 2, Action.RemoveCard.class)),
    MOVE_ENGINE_3_FORWARD(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class)),
    PLACE_BUILDING_FOR_FREE(PossibleAction.optional(Action.PlaceBuildingForFree.class));

    private final PossibleAction possibleAction;

    static Queue<MediumTownTile> shuffledPile(@NonNull Random random) {
        var list = Arrays.stream(values())
                .flatMap(tile -> IntStream.range(0, 2).mapToObj(i -> tile))
                .collect(Collectors.toList());
        Collections.shuffle(list, random);
        return new LinkedList<>(list);
    }

    ImmediateActions activate(GWT game) {
        return ImmediateActions.of(PossibleAction.optional(possibleAction.clone()));
    }
}
