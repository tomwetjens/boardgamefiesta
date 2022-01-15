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

import lombok.*;

import java.util.regex.Pattern;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
public class Hex {
    int q;
    int r;

    @Override
    public String toString() {
        return "(" + q + "," + r + ")";
    }

    private static final Pattern VALUE_OF = Pattern.compile("[(),]");

    public static Hex valueOf(String str) {
        var parts = VALUE_OF.split(str, 4);
        if (parts.length != 4) throw new IllegalArgumentException("invalid hex: " + str);
        return new Hex(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    int distance(Hex other) {
        return (Math.abs(q - other.q)
                + Math.abs(q + r - other.q - other.r)
                + Math.abs(r - other.r)) / 2;
    }

    boolean isAdjacent(Hex hex) {
        return distance(hex) == 1;
    }
}
