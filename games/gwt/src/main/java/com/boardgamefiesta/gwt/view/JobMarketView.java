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

import com.boardgamefiesta.gwt.logic.JobMarket;
import com.boardgamefiesta.gwt.logic.Worker;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class JobMarketView {

    List<RowView> rows;
    int rowLimit;
    int currentRowIndex;

    JobMarketView(JobMarket jobMarket, int playerCount) {
        rowLimit = playerCount;
        currentRowIndex = jobMarket.getCurrentRowIndex();
        rows = IntStream.rangeClosed(0, jobMarket.getCurrentRowIndex())
                .mapToObj(rowIndex -> new RowView(jobMarket.getRow(rowIndex), rowIndex))
                .collect(Collectors.toList());
    }

    @Value
    public class RowView {

        int cost;
        List<Worker> workers;
        boolean cattleMarket;

        RowView(List<Worker> row, int rowIndex) {
            cost = JobMarket.getCost(rowIndex);
            workers = row;
            cattleMarket = JobMarket.isCattleMarket(rowIndex);
        }
    }
}
