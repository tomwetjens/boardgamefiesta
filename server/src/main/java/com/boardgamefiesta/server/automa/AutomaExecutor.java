package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
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
            var tableId = request.getTableId();

            var retries = 0;
            var done = false;
            do {
                var table = tables.findById(tableId)
                        .orElseThrow(() -> new IllegalStateException("Table '" + request.getTableId().getId() + "' not found"));

                if (table.getStatus() != Table.Status.STARTED) {
                    throw new IllegalStateException("Table '" + table.getId().getId() + "' not started");
                }

                var player = table.getPlayerById(request.getPlayerId())
                        .orElseThrow(() -> new IllegalStateException("Player '" + request.getPlayerId().getId() + "' not in table '" + tableId.getId() + "'"));

                if (player.getType() != Player.Type.COMPUTER) {
                    throw new IllegalStateException("Player '" + player.getId().getId() + "' in table '" + table.getId().getId() + "' not a computer");
                }

                try {
                    if (!table.getCurrentPlayers().contains(player)) {
                        throw new IllegalStateException("Player '" + player.getId().getId() + "' in table '" + table.getId().getId() + "' not current player");
                    }

                    table.executeAutoma(player);

                    tables.update(table);
                    done = true;
                } catch (IllegalStateException | Tables.ConcurrentModificationException e) {
                    if (retries >= 20) {
                        throw new RuntimeException("Executor failed after " + retries + " retries. Table id " + table.getId().getId() + ", version " + table.getVersion(), e);
                    }

                    retries++;

                    Thread.sleep(200);
                }
            } while (!done);
        } catch (RuntimeException | InterruptedException e) {
            log.error("Error executing request", e);
        }
    }

}
