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

import com.boardgamefiesta.api.domain.InGameEvent;
import com.boardgamefiesta.api.domain.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public class Event implements InGameEvent {

    @Getter
    private final Player player;

    @Getter
    private final AnimalType animalType;

    private final Type type;

    @Getter
    private final List<String> parameters;

    @Override
    public String getType() {
        return type.name();
    }

    public enum Type {
        ADAPTATION, REGRESSION, REMOVE_ELEMENT_FROM_ANIMAL, SKIP_ELEMENT_TYPE, ABUNDANCE, DEPLETION, GLACIATION, WASTELAND, REMOVE_ELEMENT, SPECIES_REMOVED, GAIN_BONUS_VPS, SPECIATION, WANDERLUST, ADD_ELEMENT, MOVE_SPECIES, MIGRATION, COMPETITION, SCORE_TILE, DOMINATION, GAIN_VPS, CARD, SELECT_ELEMENT, EXECUTION_PHASE, EXECUTING, RESET_PHASE, PLANNING_PHASE, ADD_ELEMENT_TO_ANIMAL, GAIN_ACTION_PAWN, INITIATIVE, PLACE_ACTION_PAWN
    }
}
