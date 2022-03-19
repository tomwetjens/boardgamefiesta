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

package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Foresights;
import com.boardgamefiesta.gwt.logic.KansasCitySupply;
import com.boardgamefiesta.gwt.logic.Teepee;
import com.boardgamefiesta.gwt.logic.Worker;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class ForesightsView {

    List<List<TileView>> choices;

    ForesightsView(Foresights foresights) {
        choices = Arrays.asList(
                foresights.choices(0).stream().map(TileView::of).collect(Collectors.toList()),
                foresights.choices(1).stream().map(TileView::of).collect(Collectors.toList()),
                foresights.choices(2).stream().map(TileView::of).collect(Collectors.toList())
        );
    }

    @Getter
    public static class TileView {

        Worker worker;
        HazardView hazard;
        Teepee teepee;

        TileView(KansasCitySupply.Tile tile) {
            if (tile.getWorker() != null) {
                worker = tile.getWorker();
            } else if (tile.getHazard() != null) {
                hazard = new HazardView(tile.getHazard());
            } else {
                teepee = tile.getTeepee();
            }
        }

        public static TileView of(KansasCitySupply.Tile tile) {
            if (tile == null) {
                return null;
            }
            return new TileView(tile);
        }
    }
}
