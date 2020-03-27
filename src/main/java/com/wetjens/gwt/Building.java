package com.wetjens.gwt;

import lombok.Getter;

abstract class Building {

    @Getter
    private final Fee fee;

    protected Building(Fee fee) {
        this.fee = fee;
    }

    abstract PossibleAction getPossibleAction();
}
