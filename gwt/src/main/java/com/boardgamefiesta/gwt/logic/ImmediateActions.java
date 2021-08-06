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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ImmediateActions {

    private final List<PossibleAction> actions;

    private ImmediateActions(List<PossibleAction> actions) {
        this.actions = actions;
    }

    static ImmediateActions of(PossibleAction possibleAction) {
        return new ImmediateActions(Arrays.asList(possibleAction));
    }

    static ImmediateActions none() {
        return new ImmediateActions(Collections.emptyList());
    }

    List<PossibleAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    ImmediateActions andThen(PossibleAction possibleAction) {
        return new ImmediateActions(Stream.concat(actions.stream(), Stream.of(possibleAction))
                .collect(Collectors.toUnmodifiableList()));
    }

    ImmediateActions andThen(ImmediateActions other) {
        return new ImmediateActions(Stream.concat(actions.stream(), other.actions.stream())
                .collect(Collectors.toUnmodifiableList()));
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

}
