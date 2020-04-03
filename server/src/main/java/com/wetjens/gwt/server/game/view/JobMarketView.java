package com.wetjens.gwt.server.game.view;

import com.wetjens.gwt.JobMarket;
import com.wetjens.gwt.Worker;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
public class JobMarketView {

    List<RowView> rows;

    JobMarketView(JobMarket jobMarket) {
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
