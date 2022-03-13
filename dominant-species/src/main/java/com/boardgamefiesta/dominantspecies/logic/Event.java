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
        // Phases
        PLANNING_PHASE, // [round]
        EXECUTION_PHASE, // [round]
        EXECUTING, // [actionType]
        RESET_PHASE, // [round]
        EXTINCTION, // [species, hex, tileType]

        // Actions
        PLACE_ACTION_PAWN, // [actionType, position]
        INITIATIVE, // [position]
        ABUNDANCE, // [elementType, corner]
        ADAPTATION, // [elementType]
        DEPLETION, // [elementType, corner]
        MIGRATION, // [species, fromHex, fromTileType, toHex, toTileType]
        REGRESSION, // [#APs, #regressionBoxElements]
        SKIP_REGRESSION_OF_ELEMENT, // [elementType]
        GLACIATION, // [hex, tileType]
        SPECIATION, // [species, hex, tileType]
        WANDERLUST, // [stack, tileType, hex]
        COMPETITION, // [animalType, hex, tileType]
        WASTELAND, // [elementType]
        DOMINATION, // [hex, tileType, dominant?]
        CARD, // [card]

        // Cards
        HIBERNATION, // [species, hex, tileType]
        CATASTROPHE, // [animalTypeToKeep, hex, tileType]

        // Changes on Animal
        GAIN_ACTION_PAWN, // []
        REMOVE_ACTION_PAWN, // []
        ADD_ELEMENT_TO_ANIMAL, // [elementType]
        REMOVE_ELEMENT_FROM_ANIMAL, // [elementType]
        GAIN_VPS, // [vps]
        GAIN_VPS_FROM_TILE, // [vps, hex, tileType]
        GAIN_BONUS_VPS, // [vps]
        LOSE_VPS, // [vps]

        // Changes on Earth
        ADD_ELEMENT, // [elementType, corner]
        REMOVE_ELEMENT, // [elementType, corner]
        ADD_SPECIES, // [species, hex, tileType]
        REMOVE_SPECIES, // [animalType, species, hex, tileType]
        ELIMINATE_SPECIES, // [animalType, species, hex, tileType]
        MOVE_SPECIES, // [animalType, species, fromHex, fromTileType, toHex, toTileType]
        FINAL_SCORING,
        SAVE_FROM_EXTINCTION
    }
}
