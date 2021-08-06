/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
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
        event.getTable().ifPresent(table -> {
            if (table.getStatus() == Table.Status.STARTED) {
                table.getCurrentPlayers().stream()
                        .filter(player -> player.getType() == Player.Type.COMPUTER)
                        .forEach(player -> automaScheduler.schedule(table.getId(), player.getId()));
            }
        });
    }

}
