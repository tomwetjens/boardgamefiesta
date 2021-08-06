/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.gwt.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum City {

    KANSAS_CITY(0, EnumSet.of(DiscColor.WHITE)),
    TOPEKA(1, EnumSet.of(DiscColor.WHITE)),
    WICHITA(4, EnumSet.of(DiscColor.WHITE)),
    COLORADO_SPRINGS(6, EnumSet.of(DiscColor.WHITE)),
    SANTA_FE(8, EnumSet.of(DiscColor.WHITE)),
    ALBUQUERQUE(10, EnumSet.of(DiscColor.BLACK, DiscColor.WHITE)),
    EL_PASO(12, EnumSet.of(DiscColor.BLACK, DiscColor.WHITE)),
    SAN_DIEGO(14, EnumSet.of(DiscColor.WHITE)),
    SACRAMENTO(16, EnumSet.of(DiscColor.BLACK, DiscColor.WHITE)),
    SAN_FRANCISCO(18, EnumSet.of(DiscColor.BLACK, DiscColor.WHITE)),

    // Rails to the North expansion:
    COLUMBIA(1, EnumSet.of(DiscColor.WHITE)),
    ST_LOUIS(4, EnumSet.of(DiscColor.WHITE)),
    CHICAGO(6, EnumSet.of(DiscColor.WHITE)),
    DETROIT(10, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)),
    CLEVELAND(12, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)),
    PITTSBURGH(15, EnumSet.of(DiscColor.WHITE)),
    NEW_YORK_CITY(18, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)),
    MEMPHIS(3, EnumSet.of(DiscColor.WHITE)),
    DENVER(8, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)),
    MILWAUKEE(11, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)),
    GREEN_BAY(12, EnumSet.of(DiscColor.WHITE)),
    MINNEAPOLIS(13, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)),
    TORONTO(14, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)),
    MONTREAL(20, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK));

    @Getter
    private final int value;

    private final Set<DiscColor> discColors;

    public Set<DiscColor> getDiscColors() {
        return Collections.unmodifiableSet(discColors);
    }

    public boolean isMultipleDeliveries() {
        return this == KANSAS_CITY || this == SAN_FRANCISCO;
    }
}
