package com.wetjens.gwt;

import java.util.*;
import java.util.stream.Stream;

import lombok.Getter;

public final class JobMarket {

    @Getter
    private final int rowLimit;

    @Getter
    private final List<Row> rows;

    @Getter
    private int currentRowIndex;

    JobMarket(int playerCount) {
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

    void addWorker(Worker worker) {
        if (isClosed()) {
            throw new IllegalStateException("Job market closed");
        }

        Row row = rows.get(currentRowIndex);

        row.workers.add(worker);

        if (row.workers.size() == rowLimit) {
            currentRowIndex++;
        }
    }

    public boolean isClosed() {
        return currentRowIndex >= rows.size();
    }

    boolean fillUpCattleMarket() {
        return rows.get(currentRowIndex).cattleMarket;
    }

    void takeWorker(Worker worker) {
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
        @Getter
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

        public List<Worker> getWorkers() {
            return Collections.unmodifiableList(workers);
        }
    }
}
