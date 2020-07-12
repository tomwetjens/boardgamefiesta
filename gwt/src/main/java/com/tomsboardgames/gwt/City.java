package com.boardgamefiesta.gwt;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public enum City {

    KANSAS_CITY(true, 0, 0, Collections.singleton(DiscColor.WHITE)),
    TOPEKA(1, 0, Collections.singleton(DiscColor.WHITE)),
    WICHITA(4, 1, Collections.singleton(DiscColor.WHITE)),
    COLORADO_SPRINGS(6, 3, Collections.singleton(DiscColor.WHITE)),
    SANTA_FE(8, 4, Collections.singleton(DiscColor.WHITE)),
    ALBUQUERQUE(10, 5, Arrays.asList(DiscColor.BLACK, DiscColor.WHITE)),
    EL_PASO(12, 7, Arrays.asList(DiscColor.BLACK, DiscColor.WHITE)),
    SAN_DIEGO(14, 8, Collections.singleton(DiscColor.WHITE)),
    SACRAMENTO(16, 9, Arrays.asList(DiscColor.BLACK, DiscColor.WHITE)),
    SAN_FRANCISCO(true, 18, 11, Arrays.asList(DiscColor.BLACK, DiscColor.WHITE));

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

    public Collection<DiscColor> getDiscColors() {
        return Collections.unmodifiableCollection(discColors);
    }
}
