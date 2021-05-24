package com.boardgamefiesta.istanbul.logic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public enum MosqueTile {

    TURN_OR_REROLL_DICE(GoodsType.FABRIC, game -> ActionResult.none(true)),

    PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD(GoodsType.SPICE, game -> ActionResult.none(true)),

    EXTRA_ASSISTANT(GoodsType.BLUE, game -> {
        game.getCurrentMerchant().returnAssistants(1);
        return ActionResult.none(true);
    }),

    PAY_2_LIRA_TO_RETURN_ASSISTANT(GoodsType.FRUIT, game -> ActionResult.none(true));

    @Getter
    private final GoodsType goodsType;
    private final Function<Istanbul, ActionResult> afterAcquireFunction;

    ActionResult afterAcquire(Istanbul game) {
        return afterAcquireFunction.apply(game);
    }

}
