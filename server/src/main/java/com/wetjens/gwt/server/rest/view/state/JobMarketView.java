package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.JobMarket;
import com.wetjens.gwt.Worker;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
public class JobMarketView {

    List<RowView> rows;
    int rowLimit;
    int currentRowIndex;

    JobMarketView(JobMarket jobMarket) {
        rowLimit = jobMarket.getRowLimit();
        currentRowIndex = jobMarket.getCurrentRowIndex();
        rows = jobMarket.getRows().stream()
                .limit(jobMarket.getCurrentRowIndex() + 1)
                .map(RowView::new)
                .collect(Collectors.toList());
    }

    @Value
    public class RowView {

        int cost;
        List<Worker> workers;
        boolean cattleMarket;

        RowView(JobMarket.Row row) {
            cost = row.getCost();
            workers = row.getWorkers();
            cattleMarket = row.isCattleMarket();
        }
    }
}
