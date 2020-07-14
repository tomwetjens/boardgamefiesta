package com.boardgamefiesta.gwt.logic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Unlockable {

    CERT_LIMIT_4(1, DiscColor.WHITE, 0),
    CERT_LIMIT_6(1, DiscColor.BLACK, 0),
    EXTRA_STEP_DOLLARS(1, DiscColor.BLACK, 0),
    EXTRA_STEP_POINTS(1, DiscColor.BLACK, 0),
    EXTRA_CARD(2, DiscColor.BLACK, 5),
    AUX_GAIN_DOLLAR(2, DiscColor.WHITE,0),
    AUX_DRAW_CARD_TO_DISCARD_CARD(2, DiscColor.WHITE, 0),
    AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT(2, DiscColor.WHITE, 0),
    AUX_PAY_TO_MOVE_ENGINE_FORWARD(2, DiscColor.WHITE, 0),
    AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD(2, DiscColor.WHITE, 0);

    private final int count;
    private final DiscColor discColor;
    private final int cost;

}
