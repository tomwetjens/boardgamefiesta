package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.server.domain.Player;
import com.boardgamefiesta.server.domain.Table;
import com.boardgamefiesta.server.domain.Tables;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
@Slf4j
@Transactional
class AutomaExecutor {

    private final Tables tables;

    @Inject
    AutomaExecutor(@NonNull Tables tables) {
        this.tables = tables;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void execute(@Observes AutomaScheduler.Request request) {
        try {
            var table = request.getTable();

            var retries = 0;
            do {
                if (table.getStatus() != Table.Status.STARTED) {
                    return;
                }

                if (table.getCurrentPlayer().getType() != Player.Type.COMPUTER) {
                    return;
                }

                table.executeAutoma();

                try {
                    tables.update(table);
                } catch (Tables.TableConcurrentlyModifiedException e) {
                    if (retries >= 5) {
                        throw new RuntimeException("Executor failed after " + retries + " retries", e);
                    }

                    retries++;
                    table = tables.findById(request.getTable().getId());
                }
            } while (true);
        } catch (RuntimeException e) {
            log.error("Error executing request", e);
            throw e;
        }
    }

}
