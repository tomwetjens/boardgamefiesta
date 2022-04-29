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
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AnimalType {

    MAMMALS(List.of(ElementType.MEAT, ElementType.MEAT)),
    REPTILES(List.of(ElementType.SUN, ElementType.SUN)),
    BIRDS(List.of(ElementType.SEED, ElementType.SEED)),
    AMPHIBIANS(List.of(ElementType.WATER, ElementType.WATER, ElementType.WATER)),
    ARACHNIDS(List.of(ElementType.GRUB, ElementType.GRUB)),
    INSECTS(List.of(ElementType.GRASS, ElementType.GRASS));

    private final List<ElementType> initialElements;

    static final List<AnimalType> FOOD_CHAIN_ORDER = List.of(
            AnimalType.MAMMALS,
            AnimalType.REPTILES,
            AnimalType.BIRDS,
            AnimalType.AMPHIBIANS,
            AnimalType.ARACHNIDS,
            AnimalType.INSECTS
    );
}
