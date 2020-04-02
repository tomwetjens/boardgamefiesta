package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Getter;

public enum City {

    KANSAS_CITY(true, 0, 0, Constants.ONLY_WHITE),
    TOPEKA(1, 0, Constants.ONLY_WHITE),
    WICHITA(4, 1, Constants.ONLY_WHITE),
    COLORADO_SPRINGS(6, 3, Constants.ONLY_WHITE),
    SANTA_FE(8, 4, Constants.ONLY_WHITE),
    ALBUQUERQUE(10, 5, Constants.BLACK_OR_WHITE),
    EL_PASO(12, 7, Constants.BLACK_OR_WHITE),
    SAN_DIEGO(14, 8, Constants.ONLY_WHITE),
    SACRAMENTO(16, 9, Constants.BLACK_OR_WHITE),
    SAN_FRANCISCO(true, 18, 11, Constants.BLACK_OR_WHITE);

    @Getter private boolean multipleDeliveries;
    @Getter private int value;
    @Getter private int signals;

    private Collection<DiscColor> discColors;

    City(int value, int signals, Collection<DiscColor> discColors) {
        this(false, value, signals, discColors);
    }

    City(boolean multipleDeliveries, int value, int signals, Collection<DiscColor> discColors) {
        this.multipleDeliveries = multipleDeliveries;
        this.value = value;
        this.signals = signals;
        this.discColors = discColors;
    }

    boolean accepts(DiscColor discColor) {
        return discColors.contains(discColor);
    }

    private static class Constants {
        private static final Set<DiscColor> ONLY_WHITE = Collections.singleton(DiscColor.WHITE);
        private static final List<DiscColor> BLACK_OR_WHITE = Arrays.asList(DiscColor.BLACK, DiscColor.WHITE);
    }
}
