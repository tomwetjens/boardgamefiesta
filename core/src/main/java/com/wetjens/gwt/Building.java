package com.wetjens.gwt;

import lombok.Getter;

public abstract class Building {

    @Getter
    private final Hand hand;

    Building(Hand hand) {
        this.hand = hand;
    }

    abstract PossibleAction getPossibleAction();

}
