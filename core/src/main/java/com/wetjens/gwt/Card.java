package com.wetjens.gwt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Card {

    // Not a @Value because each instance is unique
    @AllArgsConstructor
    @Getter
    @ToString
    public static final class CattleCard extends Card {
        CattleType type;
        int points;
    }
}
