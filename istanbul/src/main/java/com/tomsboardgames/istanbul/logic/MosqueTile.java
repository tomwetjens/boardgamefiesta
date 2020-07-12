package com.boardgamefiesta.istanbul.logic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public enum MosqueTile {

    TURN_OR_REROLL_DICE(GoodsType.FABRIC, game -> ActionResult.none()),

    PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD(GoodsType.SPICE, game -> ActionResult.none()),

    EXTRA_ASSISTANT(GoodsType.BLUE, game -> {
        game.getCurrentMerchant().returnAssistants(1);
        return ActionResult.none();
    }),

    PAY_2_LIRA_TO_RETURN_ASSISTANT(GoodsType.FRUIT, game -> ActionResult.none());

    @Getter
    private final GoodsType goodsType;
    private final Function<Game, ActionResult> afterAcquireFunction;

    ActionResult afterAcquire(Game game) {
        return afterAcquireFunction.apply(game);
    }

}
