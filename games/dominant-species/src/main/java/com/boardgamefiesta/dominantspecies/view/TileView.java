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

import com.boardgamefiesta.dominantspecies.logic.AnimalType;
import com.boardgamefiesta.dominantspecies.logic.Hex;
import com.boardgamefiesta.dominantspecies.logic.Tile;
import com.boardgamefiesta.dominantspecies.logic.TileType;
import lombok.Data;

import java.util.Map;

@Data
public class TileView {

    Hex hex;
    Map<AnimalType, Integer> species;
    TileType type;
    boolean tundra;
    AnimalType dominant;

    public TileView(Hex hex, Tile tile) {
        this.hex = hex;
        this.species = tile.getSpecies();
        this.type = tile.getType();
        this.tundra = tile.isTundra();
        this.dominant = tile.getDominant().orElse(null);
    }
}
