package com.tomsboardgames.gwt;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Card implements Serializable {

    private static final long serialVersionUID = 1L;

    // Not a @Value because each instance is unique
    @AllArgsConstructor
    @Getter
    @ToString
    public static final class CattleCard extends Card {

        private static final long serialVersionUID = 1L;

        CattleType type;
        int points;
    }
}
