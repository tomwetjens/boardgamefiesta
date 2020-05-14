package com.wetjens.gwt.server.domain;

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
    public void execute(@Observes AutomaScheduler.Request request) {
        try {
            var table = request.getTable();

            if (table.getStatus() != Table.Status.STARTED) {
                return;
            }

            var currentPlayer = table.getCurrentPlayer();
            if (currentPlayer.getType() != Player.Type.COMPUTER) {
                return;
            }

            table.executeAutoma();

            tables.update(table);
            log.debug("saved after automa");
        } catch (RuntimeException e) {
            log.error("Error executing request", e);
            throw e;
        }
    }

}
