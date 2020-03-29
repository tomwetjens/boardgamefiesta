package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Unlockable {

    CERT_LIMIT_4(1, DiscColor.WHITE),
    CERT_LIMIT_6(1, DiscColor.BLACK),
    EXTRA_STEP_DOLLARS(1, DiscColor.BLACK),
    EXTRA_STEP_POINTS(1, DiscColor.BLACK),
    EXTRA_CARD(2, DiscColor.BLACK),
    AUX_GAIN_DOLLAR(1, DiscColor.WHITE),
    AUX_DRAW_CARD_TO_DISCARD_CARD(1, DiscColor.WHITE),
    AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT(2, DiscColor.WHITE),
    AUX_PAY_TO_MOVE_ENGINE_FORWARD(2, DiscColor.WHITE),
    AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD(2, DiscColor.WHITE);

    private final int count;
    private final DiscColor discColor;
}
