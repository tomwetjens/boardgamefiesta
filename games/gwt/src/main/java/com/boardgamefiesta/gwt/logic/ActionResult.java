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

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class ActionResult {
    List<PossibleAction> newActions;
    ImmediateActions immediateActions;
    boolean canUndo;

    static ActionResult undoAllowed(PossibleAction followUpAction) {
        return new ActionResult(Collections.singletonList(followUpAction), ImmediateActions.none(), true);
    }

    static ActionResult undoAllowed(ImmediateActions immediateActions) {
        return new ActionResult(Collections.emptyList(), immediateActions, true);
    }

    static ActionResult undoNotAllowed(ImmediateActions immediateActions) {
        return new ActionResult(Collections.emptyList(), immediateActions, false);
    }

    public boolean canUndo() {
        return canUndo;
    }
}
