package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.rating.Ranking;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RatingDynamoDbRepositoryV2Test extends BaseDynamoDbRepositoryTest {

    // Randomize game ID to isolate test runs
    static final Game.Id GAME_ID = Game.Id.of(UUID.randomUUID().toString());

    // Prefix for deterministic order within tests
    static final User.Id USER_ID_A = User.Id.of("A" + UUID.randomUUID().toString());
    static final User.Id USER_ID_B = User.Id.of("B" + UUID.randomUUID().toString());

    static final Table.Id TABLE_ID_A = Table.Id.of(UUID.randomUUID().toString());
    static final Table.Id TABLE_ID_B = Table.Id.of(UUID.randomUUID().toString());

    RatingDynamoDbRepositoryV2 repository;

    @BeforeEach
    void setUp() {
        repository = new RatingDynamoDbRepositoryV2(dynamoDbClient, config);
    }

    @Test
    void rankingAfterMultipleGames() {
        repository.addAll(List.of(
                rating(USER_ID_A, TABLE_ID_A, 0, 1016),
                rating(USER_ID_B, TABLE_ID_A, 0, 984)));
        repository.addAll(List.of(
                rating(USER_ID_A, TABLE_ID_B, 1, 1032),
                rating(USER_ID_B, TABLE_ID_B, 1, 968)));

        var ranking = repository.findRanking(GAME_ID, 10).collect(Collectors.toList());

        assertThat(ranking).containsExactly(
                ranking(USER_ID_A, 1032),
                ranking(USER_ID_B, 968));
    }

    @Test
    void sameRatingAtSameTime() {
        repository.addAll(List.of(
                rating(USER_ID_A, TABLE_ID_A, 0, 1016),
                rating(USER_ID_B, TABLE_ID_A, 0, 1016)));

        var ranking = repository.findRanking(GAME_ID, 10).collect(Collectors.toList());

        assertThat(ranking).containsExactly(
                ranking(USER_ID_B, 1016),
                ranking(USER_ID_A, 1016));
    }

    private static Ranking ranking(User.Id userId, int rating) {
        return Ranking.builder().gameId(GAME_ID).userId(userId).rating(rating).build();
    }

    private static Rating rating(User.Id userId, Table.Id tableId, int sequence, int rating) {
        return Rating.builder()
                .gameId(GAME_ID)
                .userId(userId)
                .tableId(tableId)
                .timestamp(Instant.ofEpochSecond(sequence))
                .rating(rating)
                .deltas(Collections.emptyMap())
                .build();
    }
}