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

import com.boardgamefiesta.api.domain.Player;

import java.util.Random;
import java.util.stream.Collectors;

public class Automa {

    public void perform(DominantSpecies state, Player player, Random random) {
        do {
            var possibleActions = state.possibleActions();
            if (possibleActions.isEmpty()) {
                state.endTurn(player, random);
            } else if (possibleActions.contains(Action.PlaceActionPawn.class)) {
                state.perform(player, placeActionPawn(state, random), random);
            } else {
                throw new DominantSpeciesException(DominantSpeciesError.NO_AUTOMA_ACTION);
            }
        } while (state.getCurrentPlayers().contains(player));
    }

    private Action placeActionPawn(DominantSpecies state, Random random) {
        // TODO Just pick a random action space for now
        var placements = state.getActionDisplay().possiblePlacements().collect(Collectors.toList());
        var placement = placements.get(random.nextInt(placements.size()));
        return new Action.PlaceActionPawn(placement.getActionType(), placement.getIndex());
    }

}
