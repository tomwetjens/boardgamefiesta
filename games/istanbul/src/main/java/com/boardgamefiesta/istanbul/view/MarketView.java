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

package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.istanbul.logic.GoodsType;
import com.boardgamefiesta.istanbul.logic.Place;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class MarketView extends PlaceView {

    private final List<GoodsType> demand;

    MarketView(Place.Market market) {
        super(market);

        this.demand = market.getDemand().entrySet().stream()
                .flatMap(entry -> IntStream.range(0, entry.getValue()).mapToObj(i -> entry.getKey()))
                .sorted()
                .collect(Collectors.toList());
    }

}