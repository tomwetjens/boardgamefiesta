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

package com.boardgamefiesta.domain.karma;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import lombok.NonNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
class KarmaAdjuster {

    private final Karmas karmas;
    private final Tables tables;

    @Inject
    KarmaAdjuster(@NonNull Karmas karmas,
                  @NonNull Tables tables) {
        this.karmas = karmas;
        this.tables = tables;
    }

    void left(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Left event) {
        tables.findById(event.getTableId()).ifPresent(table -> {
            if (table.hasMoreThanOneHumanPlayer()) {
                var current = karmas.current(event.getUserId(), event.getTimestamp());

                karmas.add(current.left(event.getTimestamp(), event.getTableId()));
            }
        });
    }

    void kicked(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        var current = karmas.current(event.getUserId(), event.getTimestamp());

        karmas.add(current.kicked(event.getTimestamp(), event.getTableId()));
    }

    void forcedEndTurn(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ForcedEndTurn event) {
        var current = karmas.current(event.getUserId(), event.getTimestamp());

        karmas.add(current.forcedEndTurn(event.getTimestamp(), event.getTableId()));
    }

    void tableEnded(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        tables.findById(event.getTableId()).ifPresent(table -> {
            if (table.hasMoreThanOneHumanPlayer()) {
                table.getPlayers().stream()
                        .filter(Player::isPlaying)
                        .filter(Player::isUser)
                        .map(Player::getUserId)
                        .flatMap(Optional::stream)
                        .forEach(userId -> {
                            var current = karmas.current(userId, event.getTimestamp());

                            karmas.add(current.finishedGame(event.getTimestamp(), event.getTableId()));
                        });
            }
        });
    }
}
