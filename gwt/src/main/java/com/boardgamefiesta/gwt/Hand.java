package com.boardgamefiesta.gwt;

public enum Hand {

    NONE(0, 0, 0),
    GREEN(2, 2, 1),
    BLACK(2, 1, 2),
    BOTH(4, 3, 3);

    private final int[] amounts;

    Hand(int... fees) {
        this.amounts = fees;
    }

    public int getFee(int playerCount) {
        return amounts[playerCount - 2];
    }
}
