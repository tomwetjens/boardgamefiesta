package com.wetjens.gwt;

import lombok.Getter;

public abstract class Building {

    @Getter
    private final Fee fee;

    Building(Fee fee) {
        this.fee = fee;
    }

    abstract PossibleAction getPossibleAction();
}
