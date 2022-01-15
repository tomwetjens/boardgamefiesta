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

package com.boardgamefiesta.dominantspecies.view;

import com.boardgamefiesta.dominantspecies.logic.Action;
import com.boardgamefiesta.dominantspecies.logic.DominantSpecies;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class DominantSpeciesView {

    DominantSpecies state; // TODO Replace with individual fields and hide the face down Wanderlust tiles
    List<String> actions;

    public DominantSpeciesView(DominantSpecies state) {
        this.state = state;
        this.actions = state.possibleActions().stream()
                .map(Action::getName)
                .collect(Collectors.toList());
    }
}
