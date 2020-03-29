package com.wetjens.gwt;

public enum Fee {

    NONE(0, 0, 0),
    GREEN(2, 2, 1),
    BLACK(2, 1, 2),
    BOTH(4, 3, 3);

    private final int[] amounts;

    Fee(int... amounts) {
        this.amounts = amounts;
    }

    public int getAmount(int playerCount) {
        return amounts[playerCount - 1];
    }
}
