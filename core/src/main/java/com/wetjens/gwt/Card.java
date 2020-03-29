package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

public abstract class Card {

    @AllArgsConstructor
    @Getter
    @ToString
    public static class CattleCard extends Card {
        CattleType type;
        int points;
    }
}
