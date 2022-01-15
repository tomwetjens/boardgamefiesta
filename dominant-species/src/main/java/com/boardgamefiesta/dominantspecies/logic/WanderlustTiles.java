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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WanderlustTiles {

    private Stack[] stacks;

    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Stack {
        @NonNull
        private Queue<TileType> faceDown;
        private TileType faceUp;

        static Stack initial(List<TileType> tiles) {
            var faceDown = new LinkedList<>(tiles);
            return new Stack(faceDown, faceDown.poll());
        }

        TileType removeFaceUp() {
            if (faceUp == null) {
                throw new DominantSpeciesException(DominantSpeciesError.NO_TILE_AVAILABLE);
            }

            var tileType = faceUp;
            faceUp = null;

            return tileType;
        }

        public Optional<TileType> getFaceUp() {
            return Optional.ofNullable(faceUp);
        }

        public int size() {
            return faceDown.size() + (faceUp != null ? 1 : 0);
        }
    }

    static WanderlustTiles initial(Random random) {
        var deck = createInitialDeck(random);

        return new WanderlustTiles(IntStream.range(0, 3)
                .mapToObj(i -> deck.subList(i * 8, i * 8 + 8))
                .map(Stack::initial)
                .toArray(Stack[]::new));
    }

    public Stack getStack(int index) {
        return stacks[index];
    }

    private static List<TileType> createInitialDeck(Random random) {
        var deck = Arrays.stream(TileType.values())
                .flatMap(tileType -> IntStream.range(0, tileType == TileType.SEA ? 6 : 3)
                        .mapToObj(i -> tileType))
                .collect(Collectors.toList());
        Collections.shuffle(deck, random);
        return deck;
    }

}
