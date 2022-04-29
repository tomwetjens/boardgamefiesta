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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TileTest {

    @Nested
    class ScoreTest {

        @Test
        void sea() {
            var tile = Tile.initial(TileType.SEA, false);
            tile.addSpecies(AnimalType.MAMMALS, 4);
            tile.addSpecies(AnimalType.BIRDS, 5);
            tile.addSpecies(AnimalType.ARACHNIDS, 6);
            tile.addSpecies(AnimalType.INSECTS, 7);

            var score = tile.score();
            assertThat(score).containsExactlyInAnyOrderEntriesOf(Map.of(
                    AnimalType.MAMMALS, 2,
                    AnimalType.BIRDS, 3,
                    AnimalType.ARACHNIDS, 5,
                    AnimalType.INSECTS, 9
            ));
        }

        @Test
        void tieBrokenByDescendingFoodChainOrder() {
            var tile = Tile.initial(TileType.SEA, false);
            tile.addSpecies(AnimalType.ARACHNIDS, 7);
            tile.addSpecies(AnimalType.INSECTS, 7);

            var score = tile.score();
            assertThat(score).containsExactlyInAnyOrderEntriesOf(Map.of(
                    AnimalType.ARACHNIDS, 9,
                    AnimalType.INSECTS, 5
            ));
        }

        @Test
        void zeroSpecies() {
            var tile = Tile.initial(TileType.SEA, false);
            tile.addSpecies(AnimalType.INSECTS, 1);
            tile.removeSpecies(AnimalType.INSECTS, 1);
            tile.addSpecies(AnimalType.ARACHNIDS, 7);

            var score = tile.score();
            assertThat(score).containsExactlyInAnyOrderEntriesOf(Map.of(
                    AnimalType.ARACHNIDS, 9
            ));
        }

        @Test
        void tundra() {
            var tile = Tile.initial(TileType.SEA, true);
            tile.addSpecies(AnimalType.ARACHNIDS, 7);
            tile.addSpecies(AnimalType.INSECTS, 7);

            var score = tile.score();
            assertThat(score).containsExactlyInAnyOrderEntriesOf(Map.of(
                    AnimalType.ARACHNIDS, 1,
                    AnimalType.INSECTS, 0
            ));
        }
    }

}