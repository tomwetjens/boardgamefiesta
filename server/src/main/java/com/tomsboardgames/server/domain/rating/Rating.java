package com.tomsboardgames.server.domain.rating;

import com.tomsboardgames.api.Game;
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

    private static final float INITIAL_RATING = 0;
    private static final float MIN_RATING = 100;
    public static final float K_FACTOR = 32f;

    @NonNull
    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    Game.Id gameId;

    Table.Id tableId;

    float rating;

    @NonNull
    Map<User.Id, Float> deltas;

    public static Rating initial(User.Id userId, Game.Id gameId) {
        return initial(userId, gameId, INITIAL_RATING);
    }

    static Rating initial(User.Id userId, Game.Id gameId, float rating) {
        return new Rating(userId, Instant.now(), gameId, null, rating, Collections.emptyMap());
    }

    public Optional<Table.Id> getTableId() {
        return Optional.ofNullable(tableId);
    }

    public Rating adjust(Collection<Rating> currentRatings, Table.Id tableId, Map<User.Id, Integer> scores, int score) {
        var opponents = currentRatings.stream()
                .filter(rating -> !rating.getUserId().equals(this.userId))
                .collect(Collectors.toMap(Rating::getUserId, Function.identity()));

        var numberOfOpponents = opponents.size();

        var kFactor = calculateKFactor(numberOfOpponents);

        // Assume 1v1 sub matches for now, no teams
        // So each player's score is considered against each other player's score individually

        var expectedScores = currentRatings.stream()
                .collect(Collectors.toMap(Rating::getUserId, this::expectedAgainst));
        var actualScores = currentRatings.stream()
                .collect(Collectors.toMap(Rating::getUserId, other -> actualScore(score, scores.get(other.getUserId()))));

        var deltas = opponents.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    var actualScore = actualScores.get(entry.getKey());
                    var expectedScore = expectedScores.get(entry.getKey());

                    var delta = kFactor * (actualScore - expectedScore);

                    // Below a certain rating, a player cannot lose any points
                    if (delta < 0 && rating <= MIN_RATING) {
                        return 0f;
                    }
                    return delta;
                }));

        var rating = this.rating + deltas.values().stream().reduce(Float::sum).orElse(0f);

        return new Rating(this.userId, Instant.now(), this.gameId, tableId, rating, deltas);
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
        return K_FACTOR / numberOfOpponents;
    }

}
