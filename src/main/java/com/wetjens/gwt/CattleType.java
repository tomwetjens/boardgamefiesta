package com.wetjens.gwt;

import lombok.Getter;

public enum CattleType {
    DUTCH_BELT(3),
    BLACK_ANGUS(2),
    GUERNSEY(1),
    JERSEY(1),
    HOLSTEIN(3),
    BROWN_SWISS(3),
    AYRSHIRE(3),
    WEST_HIGHLAND(4),
    TEXAS_LONGHORN(5);

    @Getter
    private final int value;

    CattleType(int value) {
        this.value = value;
    }
}
