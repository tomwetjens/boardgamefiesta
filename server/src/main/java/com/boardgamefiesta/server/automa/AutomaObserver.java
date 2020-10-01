package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.server.domain.Player;
import com.boardgamefiesta.server.domain.Table;
import com.boardgamefiesta.server.domain.Tables;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

@ApplicationScoped
@Slf4j
class AutomaObserver {

    private final Tables tables;
    private final AutomaScheduler automaScheduler;

    @Inject
    AutomaObserver(@NonNull Tables tables,
                   @NonNull AutomaScheduler automaScheduler) {
        this.tables = tables;
        this.automaScheduler = automaScheduler;
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.StateChanged event) {
        var table = tables.findById(event.getTableId(), true);

        if (table.getStatus() == Table.Status.STARTED
                && table.getCurrentPlayer().getType() == Player.Type.COMPUTER) {
            automaScheduler.schedule(table);
        }
    }

}
