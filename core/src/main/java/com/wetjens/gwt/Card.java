package com.wetjens.gwt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Card implements Serializable {

    private static final long serialVersionUID = 1L;

    // Not a @Value because each instance is unique
    @AllArgsConstructor
    @Getter
    @ToString
    public static final class CattleCard extends Card {
        CattleType type;
        int points;
    }
}
