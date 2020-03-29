package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import lombok.Getter;

public class JobMarket {

    @Getter
    private final int rowLimit;

    @Getter
    private final List<Row> rows;

    @Getter
    private int currentRowIndex;

    public JobMarket(int playerCount) {
        this.rowLimit = playerCount;

        this.rows = Arrays.asList(
                new Row(6),
                new Row(6),
                new Row(7),
                new Row(5),
                new Row(7),
                new Row(9, true),
                new Row(6),
                new Row(8),
                new Row(10, true),
                new Row(6),
                new Row(5),
                new Row(4));

        this.currentRowIndex = 1;
    }

    public boolean addWorker(Worker worker) {
        Row row = rows.get(currentRowIndex);

        row.workers.add(worker);

        if (row.workers.size() == rowLimit) {
            currentRowIndex++;

            return row.cattleMarket;
        }

        return false;
    }

    public void takeWorker(Worker worker) {
        cheapestRow(worker).workers.remove(worker);
    }

    public int cost(Worker worker) {
        return cheapestRow(worker).getCost();
    }

    private Stream<Row> availableRows() {
        return rows.stream().limit(currentRowIndex);
    }

    private Row cheapestRow(Worker worker) {
        return availableRows()
                .filter(row -> row.workers.contains(worker))
                .min(Comparator.comparingInt(Row::getCost))
                .orElseThrow(() -> new IllegalStateException("No " + worker + " available in job market"));
    }

    public final class Row {

        @Getter
        private final int cost;
        private final boolean cattleMarket;
        private final List<Worker> workers;

        private Row(int cost) {
            this(cost, false);
        }

        private Row(int cost, boolean cattleMarket) {
            this.cost = cost;
            this.cattleMarket = cattleMarket;
            this.workers = new ArrayList<>(rowLimit);
        }
    }
}
