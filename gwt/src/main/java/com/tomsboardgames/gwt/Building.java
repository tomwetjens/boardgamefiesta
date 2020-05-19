package com.tomsboardgames.gwt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Building implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String name;

    @Getter
    private final Hand hand;

    abstract PossibleAction getPossibleAction(Game game);

}
