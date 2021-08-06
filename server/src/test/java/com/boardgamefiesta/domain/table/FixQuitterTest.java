package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.rating.RatingAdjuster;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.RatingDynamoDbRepositoryV2;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepositoryV2;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepositoryV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
class FixQuitterTest {

    @Mock
    CDI<Object> cdi;
    @Mock
    BeanManager beanManager;

    @Mock
    DynamoDbConfiguration config;

    Users users;
    Ratings ratings;
    Tables tables;

    RatingAdjuster ratingAdjuster;

    @BeforeEach
    void setUp() {
        lenient().when(cdi.getBeanManager()).thenReturn(beanManager);
        CDI.setCDIProvider(() -> cdi);

        lenient().when(config.getTableName()).thenReturn("boardgamefiesta-prod");
        lenient().when(config.getReadGameIdShards()).thenReturn(2);
        lenient().when(config.getWriteGameIdShards()).thenReturn(2);

        var dynamoDbClient = DynamoDbClient.create();
        users = new UserDynamoDbRepositoryV2(dynamoDbClient, config);
        ratings = new RatingDynamoDbRepositoryV2(dynamoDbClient, config);
        tables = new TableDynamoDbRepositoryV2(new Games(), dynamoDbClient, config);

        ratingAdjuster = new RatingAdjuster(tables, ratings);
    }

    @Test
    void singleTable() {
        tables.findById(Table.Id.of("c9cd16fc-fab4-4ba4-b616-032dd43a3bc6"))
                .ifPresent(this::fix);
    }

    private void fix(Table table) {
        var playersThatLeft = table.getPlayers().stream()
                .filter(player -> player.getStatus() == Player.Status.LEFT)
                .collect(Collectors.toSet());

        // Do logic again
        playersThatLeft.forEach(table::afterPlayerLeft);

        var adjustedRatings = ratingAdjuster.adjustRatings(table);

        tables.update(table);

        ratings.addAll(adjustedRatings);
    }

}