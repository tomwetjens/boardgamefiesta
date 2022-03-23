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

package com.boardgamefiesta.domain.rating;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adjusts a user's rating after a game ends, based on the actual scores.
 */
@ApplicationScoped
@Slf4j
public class RatingAdjuster {

    private final Tables tables;
    private final Ratings ratings;

    @Inject
    public RatingAdjuster(@NonNull Tables tables, @NonNull Ratings ratings) {
        this.tables = tables;
        this.ratings = ratings;
    }

    public void tableEnded(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        try {
            tables.findById(event.getTableId()).ifPresent(table -> {
                var result = adjustRatings(table);

                if (!result.isEmpty()) {
                    ratings.addAll(result);
                }
            });
        } catch (RuntimeException e) {
            log.error("Error while adjusting rating after: {}", event, e);
        }
    }

    public Set<Rating> adjustRatings(Table table) {
        if (table.getStatus() != Table.Status.ENDED) {
            // Ignore
            return Collections.emptySet();
        }

        if (table.getMode() != Table.Mode.NORMAL) {
            // Ignore training mode
            return Collections.emptySet();
        }

        if (table.hasComputerPlayers()) {
            // Games with computer players are not eligible for rating
            return Collections.emptySet();
        }

        var playerRatings = table.getPlayers().stream()
                // Only users can have ratings
                .flatMap(player -> player.getUserId().stream())
                .collect(Collectors.toMap(Function.identity(), userId -> ratings.findLatest(userId, table.getGame().getId(), table.getEnded())));

        return playerRatings.values().stream()
                .map(rating -> rating.adjust(playerRatings, table))
                // If no deltas (no change), then no need to add to repository
                .filter(rating -> !rating.getDeltas().isEmpty())
                .collect(Collectors.toSet());
    }

}
