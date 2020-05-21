package com.tomsboardgames.server.domain.rating;

import com.tomsboardgames.server.domain.Table;
import com.tomsboardgames.server.domain.User;
import lombok.*;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rating {

    private static final float INITIAL_RATING = 100;

    @NonNull
    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    String gameId;

    Table.Id tableId;

    float rating;

    @NonNull
    Map<User.Id, Float> deltas;

    @NonNull
    Instant expires;

    public static Rating initial(User.Id userId, String gameId) {
        return initial(userId, gameId, INITIAL_RATING);
    }

    static Rating initial(User.Id userId, String gameId, float rating) {
        return new Rating(userId, Instant.now(), gameId, null, rating, Collections.emptyMap(), Instant.MAX);
    }

    public Optional<Table.Id> getTableId() {
        return Optional.ofNullable(tableId);
    }

    public Rating adjust(Collection<Rating> currentRatings, Table.Id tableId, Map<User.Id, Integer> scores, int score) {
        var opponents = currentRatings.stream()
                .filter(rating -> !rating.getUserId().equals(this.userId))
                .map(Rating::getUserId)
                .collect(Collectors.toSet());

        var numberOfOpponents = opponents.size();

        var kFactor = calculateKFactor(numberOfOpponents);

        // Assume 1v1 sub matches for now, no teams
        // So each player's score is considered against each other player's score individually

        var expectedScores = currentRatings.stream()
                .collect(Collectors.toMap(Rating::getUserId, this::expectedAgainst));
        var actualScores = currentRatings.stream()
                .collect(Collectors.toMap(Rating::getUserId, other -> actualScore(score, scores.get(other.getUserId()))));

        var deltas = opponents.stream()
                .collect(Collectors.toMap(Function.identity(), opponent -> {
                    var actualScore = actualScores.get(opponent);
                    var expectedScore = expectedScores.get(opponent);
                    return kFactor * (actualScore - expectedScore);
                }));

        var rating = this.rating + deltas.values().stream().reduce(Float::sum).orElse(0f);

        return new Rating(this.userId, Instant.now(), this.gameId, tableId, rating, deltas, Instant.MAX);
    }

    public float expectedAgainst(Rating other) {
        // https://en.wikipedia.org/wiki/Elo_rating_system
        return 1f / (1f + (float) Math.pow(10, ((other.rating - rating) / 400f)));
    }

    private float actualScore(int score, int opponentScore) {
        return score == opponentScore ? 0.5f : score < opponentScore ? 0f : 1f;
    }

    private float calculateKFactor(int numberOfOpponents) {
        // TODO How to determine K factor
        // Now assuming, the more opponents a player has, the more difficult, therefore less rating to lose
        return 32f / numberOfOpponents;
    }

}
