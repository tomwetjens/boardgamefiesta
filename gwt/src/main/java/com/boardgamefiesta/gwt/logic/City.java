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
