/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.automa;

import com.boardgamefiesta.domain.DomainService;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
@Slf4j
public class AutomaExecutor implements DomainService {

    public static final int MAX_RETRIES = 30;

    private final Tables tables;

    @Inject
    AutomaExecutor(@NonNull Tables tables) {
        this.tables = tables;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(Table.Id tableId, Player.Id playerId) {
        try {
            var retries = 0;
            do {
                log.debug("Executing for table {} and player {}", tableId.getId(), playerId.getId());

                var table = tables.findById(tableId)
                        .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId.getId()));

                if (table.getStatus() != Table.Status.STARTED) {
                    return;
                }

                var player = table.getPlayerById(playerId)
                        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId.getId()));

                if (player.getType() != Player.Type.COMPUTER) {
                    return;
                }

                if (!table.getCurrentPlayers().contains(player)) {
                    return;
                }

                table.executeAutoma(player);

                try {
                    tables.update(table);
                    return;
                } catch (Tables.ConcurrentModificationException e) {
                    if (retries >= MAX_RETRIES) {
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
