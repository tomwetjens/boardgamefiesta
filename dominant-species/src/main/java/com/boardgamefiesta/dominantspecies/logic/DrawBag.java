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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DrawBag {

    @Singular
    private Map<ElementType, Integer> elements;

    public static DrawBag initial() {
        return new DrawBag(Arrays.stream(ElementType.values())
                .collect(Collectors.toMap(Function.identity(), elementType -> 18)));
    }

    ElementType draw(Random random) {
        var total = getTotal();

        if (total > 0) {
            var n = random.nextInt(total);
            for (var elementType : ElementType.values()) {
                if ((n -= elements.get(elementType)) < 0) {
                    elements.compute(elementType, (k, count) -> count - 1);
                    return elementType;
                }
            }
        }

        throw new DominantSpeciesException(DominantSpeciesError.NO_ELEMENTS_IN_DRAW_BAG);
    }

    private int getTotal() {
        return elements.values().stream().mapToInt(i -> i).sum();
    }

    List<ElementType> draw(int count, Random random) {
        return IntStream.range(0, count)
                .mapToObj(i -> draw(random))
                .collect(Collectors.toList());
    }

    void add(ElementType elementType) {
        elements.compute(elementType, (k, v) -> v + 1);
    }

    void addAll(List<ElementType> elements) {
        elements.forEach(this::add);
    }

    void remove(ElementType elementType) {
        var count = elements.get(elementType);

        if (count == null || count == 0) {
            throw new DominantSpeciesException(DominantSpeciesError.ELEMENT_NOT_IN_DRAW_BAG);
        }

        elements.put(elementType, count - 1);
    }

    boolean containsAny(Collection<ElementType> elementTypes) {
        return elementTypes.stream().anyMatch(this::contains);
    }

    boolean contains(ElementType elementType) {
        return elements.getOrDefault(elementType, 0) > 0;
    }

    int count(ElementType elementType) {
        return elements.getOrDefault(elementType, 0);
    }

    public boolean isEmpty() {
        return getTotal() == 0;
    }
}
