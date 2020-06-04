package com.tomsboardgames.istanbul.logic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public enum MosqueTile {

    TURN_OR_REROLL_DICE(GoodsType.FABRIC, game -> ActionResult.none(), game -> {
        // TODO Implement mosque tile
        return ActionResult.none();
    }),

    PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD(GoodsType.SPICE, game -> ActionResult.none(), game -> {
        // TODO Implement mosque tile
        return ActionResult.none();
    }),

    EXTRA_ASSISTANT(GoodsType.BLUE, game -> {
        // TODO Implement mosque tile
        return ActionResult.none();
    }, game -> ActionResult.none()),

    PAY_2_LIRA_TO_RETURN_ASSISTANT(GoodsType.FRUIT, game -> ActionResult.none(), game -> {
        // TODO Implement mosque tile
        return ActionResult.none();
    });

    @Getter
    private final GoodsType goodsType;
    private final Function<Game, ActionResult> afterAcquireFunction;
    private final Function<Game, ActionResult> afterPlaceActionFunction;

    ActionResult afterAcquire(Game game) {
        return afterAcquireFunction.apply(game);
    }

    ActionResult afterPlaceAction(Game game) {
        return afterPlaceActionFunction.apply(game);
    }
}
