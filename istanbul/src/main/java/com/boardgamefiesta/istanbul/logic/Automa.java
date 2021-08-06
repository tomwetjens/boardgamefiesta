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

import com.boardgamefiesta.api.domain.Player;

import java.util.Collections;
import java.util.Random;

public class Automa {

    public void execute(Istanbul state, Player player, Random random) {
        var possibleActions = state.getPossibleActions();

        if (possibleActions.contains(Action.Move.class)) {
            state.perform(player, bestPossibleMove(state, random), random);
        } else {
            // For now just end turn
            state.endTurn(player, random);
        }
    }

    private Action.Move bestPossibleMove(Istanbul state, Random random) {
        var possiblePlaces = state.possiblePlaces();
        // TODO Make smarter. Just random for now
        Collections.shuffle(possiblePlaces, random);
        return new Action.Move(possiblePlaces.get(0));
    }
}
