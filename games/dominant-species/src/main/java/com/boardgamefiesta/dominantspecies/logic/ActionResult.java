/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.dominantspecies.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Arrays;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ActionResult {

    FollowUpActions followUpActions;
    boolean canUndo;

    static ActionResult undoAllowed() {
        return new ActionResult(FollowUpActions.none(), true);
    }

    static ActionResult undoNotAllowed() {
        return new ActionResult(FollowUpActions.none(), false);
    }

    static ActionResult undoAllowed(PossibleAction... followUpActions) {
        return new ActionResult(FollowUpActions.of(Arrays.asList(followUpActions)), true);
    }

    static ActionResult undoAllowed(FollowUpActions followUpActions) {
        return new ActionResult(followUpActions, true);
    }

    boolean canUndo() {
        return canUndo;
    }

}
