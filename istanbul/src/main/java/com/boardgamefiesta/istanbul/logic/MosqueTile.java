/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
