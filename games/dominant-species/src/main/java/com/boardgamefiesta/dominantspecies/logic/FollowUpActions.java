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

import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value(staticConstructor = "of")
public class FollowUpActions {

    private static final FollowUpActions NONE = new FollowUpActions(Collections.emptyList());

    List<PossibleAction> possibleActions;

    public static FollowUpActions none() {
        return NONE;
    }

    boolean isEmpty() {
        return possibleActions.isEmpty();
    }

    void addTo(ActionQueue actionQueue) {
        actionQueue.addAll(possibleActions);
    }

    FollowUpActions concat(FollowUpActions other) {
        var list = new ArrayList<>(possibleActions);
        list.addAll(other.possibleActions);
        return new FollowUpActions(list);
    }
}
