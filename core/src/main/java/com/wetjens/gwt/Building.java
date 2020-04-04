package com.wetjens.gwt;

import lombok.Getter;

import java.io.Serializable;

public abstract class Building implements Serializable {

    @Getter
    private final Hand hand;

    Building(Hand hand) {
        this.hand = hand;
    }

    abstract PossibleAction getPossibleAction();

}
