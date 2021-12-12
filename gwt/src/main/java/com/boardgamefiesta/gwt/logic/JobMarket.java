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

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobMarket {

    @Getter
    private final List<Row> rows;

    @Getter
    private int rowLimit;

    @Getter
    private int currentRowIndex;

    JobMarket(int playerCount) {
        adjustRowLimit(playerCount);

        this.rows = Arrays.asList(
                new Row(6),
                new Row(6),
                new Row(7),
                new Row(5),
                new Row(7),
                new Row(9),
                new Row(6, true),
                new Row(8),
                new Row(10),
                new Row(6, true),
                new Row(5),
                new Row(4));

        this.currentRowIndex = 0;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("currentRowIndex", currentRowIndex)
                .add("rows", JsonSerializer.forFactory(factory).fromCollection(rows, Row::serialize))
                .build();
    }

    static JobMarket deserialize(int playerCount, JsonObject jsonObject) {
        var jobMarket = builder()
                .currentRowIndex(jsonObject.getInt("currentRowIndex"))
                .rows(jsonObject.getJsonArray("rows").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Row::deserialize)
                        .collect(Collectors.toList()))
                .build();

        jobMarket.adjustRowLimit(playerCount);

        return jobMarket;
    }

    void adjustRowLimit(int playerCount) {
        this.rowLimit = playerCount;
    }

    /**
     * @return <code>true</code> if the cattle market should be filled because of this action.
     */
    boolean addWorker(Worker worker) {
        if (isClosed()) {
            throw new GWTException(GWTError.JOB_MARKET_CLOSED);
        }

        Row row = rows.get(currentRowIndex);

        row.workers.add(worker);

        if (row.workers.size() == rowLimit) {
            currentRowIndex++;

            return currentRowIndex < rows.size() && rows.get(currentRowIndex).cattleMarket;
        }

        return false;
    }

    public boolean isClosed() {
        return currentRowIndex >= rows.size();
    }

    void takeWorker(int rowIndex, Worker worker) {
        if (rowIndex >= currentRowIndex) {
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE);
        }

        var row = rows.get(rowIndex);

        if (!row.workers.contains(worker)) {
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE);
        }

        rows.get(rowIndex).workers.remove(worker);
    }

    public int cost(int rowIndex, Worker worker) {
        if (rowIndex >= currentRowIndex) {
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE);
        }

        var row = rows.get(rowIndex);

        if (!row.workers.contains(worker)) {
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE);
        }

        return row.getCost();
    }

    public int getProgress() {
        var workersPlacedBeforeCurrentRow = (currentRowIndex - 1) * rowLimit;
        var workersPlacedCurrentRow = currentRowIndex < rows.size() ? rows.get(currentRowIndex).getWorkers().size() : 0;
        var totalWorkersPlaced = workersPlacedBeforeCurrentRow + workersPlacedCurrentRow;
        var totalWorkersCapacity = rows.size() * rowLimit;
        return Math.min(100, Math.round(((float) totalWorkersPlaced / (float) totalWorkersCapacity) * 100));
    }

    @Builder(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Row {

        @Getter
        private final int cost;

        @Getter
        private final boolean cattleMarket;

        private final List<Worker> workers;

        private Row(int cost) {
            this(cost, false);
        }

        private Row(int cost, boolean cattleMarket) {
            this.cost = cost;
            this.cattleMarket = cattleMarket;
            this.workers = new ArrayList<>(4);
        }

        public List<Worker> getWorkers() {
            return Collections.unmodifiableList(workers);
        }

        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("cost", cost)
                    .add("cattleMarket", cattleMarket)
                    .add("workers", JsonSerializer.forFactory(factory).fromStrings(workers.stream().map(Worker::name)))
                    .build();
        }

        static Row deserialize(JsonObject jsonObject) {
            return new Row(jsonObject.getInt("cost"),
                    jsonObject.getBoolean("cattleMarket"),
                    jsonObject.getJsonArray("workers").getValuesAs(jsonValue -> Worker.valueOf(((JsonString) jsonValue).getString())));
        }
    }
}
