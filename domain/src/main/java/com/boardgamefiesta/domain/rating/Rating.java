package com.boardgamefiesta.domain.rating;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
        var ranking = table.getUserRanking();
        var numberOfPlayers = ranking.size();

        // Assume 1v1 sub matches for now, no teams
        // So each player's score is considered against each other player's score individually
        var deltas = ranking.stream()
                .filter(otherUserId -> !userId.equals(otherUserId))
                .collect(Collectors.toMap(Function.identity(), opponentUserId -> {
                    var actualScore = actualScore(ranking.indexOf(userId), ranking.indexOf(opponentUserId));
                    return RATING_SYSTEM.calculateNewRating(actualScore, this, currentRatings.get(opponentUserId), numberOfPlayers) - rating;
                }));

        var rating = this.rating + deltas.values().stream().reduce(Integer::sum).orElse(0);

        return new Rating(userId, table.getEnded(), table.getGame().getId(), table.getId(), rating, deltas);
    }

    private float actualScore(int rank, int opponentRank) {
        return rank == opponentRank ? 0.5f : rank < opponentRank ? 1f : 0f;
    }

}
