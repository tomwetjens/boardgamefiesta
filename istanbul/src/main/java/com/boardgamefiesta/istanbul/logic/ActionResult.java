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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionResult {
    List<PossibleAction> followUpActions;
    boolean undo;
    boolean immediate;

    public static ActionResult none(boolean canUndo) {
        return new ActionResult(Collections.emptyList(), canUndo, false);
    }

    public static ActionResult immediate(PossibleAction possibleAction, boolean canUndo) {
        return new ActionResult(List.of(possibleAction), canUndo, true);
    }

    public static ActionResult followUp(PossibleAction possibleAction, boolean canUndo) {
        return new ActionResult(List.of(possibleAction), canUndo, false);
    }

    public ActionResult andThen(ActionResult actionResult) {
        return new ActionResult(
                Stream.concat(followUpActions.stream(), actionResult.followUpActions.stream())
                        .collect(Collectors.toList()),
                undo && actionResult.canUndo(),
                immediate || actionResult.isImmediate());
    }

    public boolean canUndo() {
        return undo;
    }
}
