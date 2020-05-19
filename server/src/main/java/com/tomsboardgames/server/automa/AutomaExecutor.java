package com.tomsboardgames.server.automa;

import com.tomsboardgames.server.domain.Player;
import com.tomsboardgames.server.domain.Table;
import com.tomsboardgames.server.domain.Tables;
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

            if (table.getStatus() != Table.Status.STARTED) {
                return;
            }

            if (table.getCurrentPlayer().getType() != Player.Type.COMPUTER) {
                return;
            }

            table.executeAutoma();

            tables.update(table);
        } catch (RuntimeException e) {
            log.error("Error executing request", e);
            throw e;
        }
    }

}
