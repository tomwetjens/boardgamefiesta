package com.boardgamefiesta.server.domain.rating;

import com.boardgamefiesta.server.domain.Player;
import com.boardgamefiesta.server.domain.Table;
import com.boardgamefiesta.server.domain.Tables;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
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
            var table = tables.findById(event.getTableId());

            if (table.getStatus() != Table.Status.ENDED) {
                // Ignore
                return;
            }

            if (table.getMode() != Table.Mode.NORMAL) {
                // Ignore training mode
                return;
            }

            var playerRatings = table.getPlayers().stream()
                    // Only users can have ratings
                    .flatMap(player -> player.getUserId().stream())
                    .map(userId -> ratings.findLatest(userId, table.getGame().getId()))
                    .collect(Collectors.toList());

            var playerScores = table.getPlayers().stream()
                    // Computer players have no rating and therefore cannot be considered
                    .filter(player -> player.getType() == Player.Type.USER)
                    .collect(Collectors.toMap(
                            player -> player.getUserId().orElseThrow(),
                            player -> player.getScore().orElseThrow()));

            ratings.addAll(playerRatings.stream()
                    .map(rating -> rating.adjust(playerRatings, table.getId(), playerScores, playerScores.get(rating.getUserId())))
                    // If no deltas (no change), then no need to add to repository
                    .filter(rating -> !rating.getDeltas().isEmpty())
                    .collect(Collectors.toList()));
        } catch (RuntimeException e) {
            log.error("Error while adjusting rating after: {}", event, e);
        }
    }

}
