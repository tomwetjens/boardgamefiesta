package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.server.domain.table.Player;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.table.Tables;
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
            var retries = 0;
            do {
                var table = tables.findById(request.getTable().getId(), true);

                if (table.getStatus() != Table.Status.STARTED) {
                    return;
                }

                if (request.getPlayer().getType() != Player.Type.COMPUTER) {
                    return;
                }

                if (!table.getCurrentPlayers().contains(request.getPlayer())) {
                    return;
                }

                table.executeAutoma(request.getPlayer());

                try {
                    tables.update(table);
                    return;
                } catch (Tables.TableConcurrentlyModifiedException e) {
                    if (retries >= 256) {
                        throw new RuntimeException("Executor failed after " + retries + " retries. Table id " + table.getId().getId() + ", version " + table.getVersion(), e);
                    }

                    retries++;
                }
            } while (true);
        } catch (RuntimeException e) {
            log.error("Error executing request", e);
            throw e;
        }
    }

}
