package com.boardgamefiesta.server.query;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.rating.RatingAdjuster;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.RatingDynamoDbRepository;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepository;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
class RecalculateAllRatingsTest {

    @Mock
    DynamoDbConfiguration config;

    UserDynamoDbRepository users;
    RatingDynamoDbRepository ratings;
    TableDynamoDbRepository tables;

    RatingAdjuster ratingAdjuster;

    @BeforeEach
    void setUp() {
        when(config.getTableSuffix()).thenReturn(Optional.of(""));

        var dynamoDbClient = DynamoDbClient.create();
        users = new UserDynamoDbRepository(dynamoDbClient, config);
        ratings = new RatingDynamoDbRepository(dynamoDbClient, config);
        tables = new TableDynamoDbRepository(new Games(), dynamoDbClient, config);

        ratingAdjuster = new RatingAdjuster(tables, ratings);
    }

    @Test
    void run() {
        tables.findAllEndedSortedByEndedAscending()
//        tables.findRecent(User.Id.of("f256e997-3b9f-47c6-85b9-01cc21e882c3"), Game.Id.of(GWT.ID), 9999)
//                .filter(table -> !table.hasComputerPlayers())
//                .filter(table -> !table.getEnded().isBefore(Instant.now().minus(1, ChronoUnit.DAYS)))
//                .limit(1)
                .forEach(table -> {
                    var oldRatings = table.getPlayers().stream()
                            .map(Player::getUserId)
                            .flatMap(Optional::stream)
                            .flatMap(userId -> ratings.findByTable(userId, table.getId()).stream())
                            .sorted(Comparator.comparing(rating -> rating.getUserId().getId()))
                            .collect(Collectors.toList());

                    System.out.println(table.getId() + " " + table.getEnded());

                    if (!table.hasComputerPlayers()) {
                        var newRatings = ratingAdjuster.adjustRatings(table).stream()
                                .sorted(Comparator.comparing(rating -> rating.getUserId().getId()))
                                .collect(Collectors.toList());

                        ratings.addAll(newRatings);

                        var oldRatingsToDelete = oldRatings.stream()
                                .filter(oldRating -> newRatings.stream()
                                        .noneMatch(newRating -> newRating.getTimestamp().toEpochMilli()
                                                == oldRating.getTimestamp().toEpochMilli()))
                                .collect(Collectors.toList());
                        oldRatingsToDelete.forEach(ratings::delete);
                    } else {
                        oldRatings.forEach(ratings::delete);
                    }
                });
    }
}