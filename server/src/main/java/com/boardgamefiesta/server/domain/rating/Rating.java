package com.boardgamefiesta.server.domain.rating;

import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.user.User;
import lombok.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rating {

    private static final RatingSystem RATING_SYSTEM = new EloRatingSystem();

    @NonNull
    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    Game.Id gameId;

    Table.Id tableId;

    int rating;

    @NonNull
    Map<User.Id, Integer> deltas;

    public static Rating initial(User.Id userId, Game.Id gameId) {
        return initial(userId, gameId, RATING_SYSTEM.getInitialRating());
    }

    static Rating initial(User.Id userId, Game.Id gameId, int rating) {
        return new Rating(userId, Instant.now(), gameId, null, rating, Collections.emptyMap());
    }

    public Optional<Table.Id> getTableId() {
        return Optional.ofNullable(tableId);
    }

    public Rating adjust(Map<User.Id, Rating> currentRatings, Table table) {
        var scores = table.getUserScores();

        var numberOfPlayers = scores.size();

        // Assume 1v1 sub matches for now, no teams
        // So each player's score is considered against each other player's score individually
        var deltas = scores.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(userId))
                .collect(Collectors.toMap(Map.Entry::getKey, opponentScore -> {
                    var actualScore = actualScore(scores.get(userId), opponentScore.getValue());
                    return RATING_SYSTEM.calculateNewRating(actualScore, this, currentRatings.get(opponentScore.getKey()), numberOfPlayers) - rating;
                }));

        var rating = this.rating + deltas.values().stream().reduce(Integer::sum).orElse(0);

        return new Rating(userId, table.getEnded(), table.getGame().getId(), table.getId(), rating, deltas);
    }

    private float actualScore(int score, int opponentScore) {
        return score == opponentScore ? 0.5f : score < opponentScore ? 0f : 1f;
    }

}
