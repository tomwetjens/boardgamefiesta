package com.wetjens.gwt;

import lombok.Getter;

public enum CattleType {

    JERSEY(1),
    DUTCH_BELT(2),
    BLACK_ANGUS(2),
    GUERNSEY(2),
    HOLSTEIN(3),
    BROWN_SWISS(3),
    AYRSHIRE(3),
    WEST_HIGHLAND(4),
    TEXAS_LONGHORN(5);

    public static final int MAX_VALUE = 5;

    @Getter
    private final int value;

    CattleType(int value) {
        this.value = value;
    }
}
