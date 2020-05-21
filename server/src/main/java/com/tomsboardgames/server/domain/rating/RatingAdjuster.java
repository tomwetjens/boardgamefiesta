package com.tomsboardgames.server.domain.rating;

import com.tomsboardgames.server.domain.Player;
import com.tomsboardgames.server.domain.Table;
import com.tomsboardgames.server.domain.Tables;
import lombok.NonNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.stream.Collectors;

@ApplicationScoped
public class RatingAdjuster {

    private final Tables tables;
    private final Ratings ratings;

    @Inject
    public RatingAdjuster(@NonNull Tables tables, @NonNull Ratings ratings) {
        this.tables = tables;
        this.ratings = ratings;
    }

    @Transactional
    public void tableEnded(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        var table = tables.findById(event.getTableId());

        var playerRatings = table.getPlayers().stream()
                .flatMap(player -> player.getUserId().stream())
                .map(userId -> ratings.findLatest(userId, table.getGame().getId()))
                .collect(Collectors.toList());

        var playerScores = table.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .collect(Collectors.toMap(
                        player -> player.getUserId().orElseThrow(),
                        player -> player.getScore().orElseThrow()));

        ratings.addAll(playerRatings.stream()
                .map(rating -> rating.adjust(playerRatings, table.getId(), playerScores, playerScores.get(rating.getUserId())))
                .collect(Collectors.toList()));
    }

}
