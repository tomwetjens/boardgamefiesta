package com.wetjens.gwt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import lombok.Getter;

public final class JobMarket implements Serializable {

    private static final long serialVersionUID = 1L;

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
                new Row(9),
                new Row(6, true),
                new Row(8),
                new Row(10),
                new Row(6, true),
                new Row(5),
                new Row(4));

        this.currentRowIndex = 0;
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
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE, worker, rowIndex);
        }

        var row = rows.get(rowIndex);

        if (!row.workers.contains(worker)) {
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE, worker, rowIndex);
        }

        rows.get(rowIndex).workers.remove(worker);
    }

    public int cost(int rowIndex, Worker worker) {
        if (rowIndex >= currentRowIndex) {
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE, worker, rowIndex);
        }

        var row = rows.get(rowIndex);

        if (!row.workers.contains(worker)) {
            throw new GWTException(GWTError.WORKER_NOT_AVAILABLE, worker, rowIndex);
        }

        return row.getCost();
    }

    public final class Row implements Serializable {

        private static final long serialVersionUID = 1L;

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
