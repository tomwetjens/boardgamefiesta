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

import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public class Corner {

    Hex a, b, c;

    public Corner(@NonNull Hex a,
                  @NonNull Hex b,
                  @NonNull Hex c) {
        if (a.equals(b) || b.equals(c) || a.equals(c)) {
            throw new IllegalArgumentException("must be different hexes");
        }

        if (!a.isAdjacent(b) || !b.isAdjacent(c) || !a.isAdjacent(c)) {
            throw new IllegalArgumentException("must be adjacent hexes");
        }

        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static Set<Hex> commonHexes(Collection<Corner> corners) {
        Set<Hex> result = null;
        for (var corner : corners) {
            if (result == null) {
                result = new HashSet<>(Set.of(corner.a, corner.b, corner.c));
            } else {
                result.retainAll(Set.of(corner.a, corner.b, corner.c));
            }
        }
        return result;
    }

    public static Optional<Hex> commonHex(Collection<Corner> corners) {
        var hexes = commonHexes(corners);
        return hexes.size() == 1 ? hexes.stream().findAny() : Optional.empty();
    }

    public boolean isAdjacent(Hex hex) {
        return a.equals(hex) || b.equals(hex) || c.equals(hex);
    }

    @Override
    public String toString() {
        return "(" + a + "," + b + "," + c + ")";
    }

    private static final Pattern VALUE_OF = Pattern.compile("[(),]+");

    public static Corner valueOf(String str) {
        var parts = VALUE_OF.split(str, 8);
        if (parts.length != 8) throw new IllegalArgumentException("invalid corner: " + str);
        return new Corner(
                new Hex(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
                new Hex(Integer.parseInt(parts[3]), Integer.parseInt(parts[4])),
                new Hex(Integer.parseInt(parts[5]), Integer.parseInt(parts[6])));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Corner corner = (Corner) o;
        return isAdjacent(corner.a) && isAdjacent(corner.b) && isAdjacent(corner.c);
    }

    @Override
    public int hashCode() {
        // Must be independent of order!
        // https://stackoverflow.com/questions/30734848/order-independent-hash-algorithm
        return a.hashCode() + b.hashCode() + c.hashCode();
    }
}
