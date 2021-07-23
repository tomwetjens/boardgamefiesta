package com.boardgamefiesta.server.query;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.rating.RatingAdjuster;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.LogEntry;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.RatingDynamoDbRepositoryV2;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepositoryV2;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepositoryV2;
import com.boardgamefiesta.gwt.GWTProvider;
import com.boardgamefiesta.gwt.logic.GWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Comparator;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
@Disabled
public class AddTurnsMigration {

    @Mock
    DynamoDbConfiguration config;

    Users users;
    Ratings ratings;
    Tables tables;
    RatingAdjuster ratingAdjuster;

    @BeforeEach
    void setUp() {
        var dynamoDbClient = DynamoDbClient.create();
        users = new UserDynamoDbRepositoryV2(dynamoDbClient, config);
        ratings = new RatingDynamoDbRepositoryV2(dynamoDbClient, config);
        tables = new TableDynamoDbRepositoryV2(new Games(), dynamoDbClient, config);

        ratingAdjuster = new RatingAdjuster(tables, ratings);
    }

    @Test
    void run() {
        tables.findEndedWithHumanPlayers(com.boardgamefiesta.domain.game.Game.Id.of(GWTProvider.ID), Integer.MAX_VALUE, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, true)
                .filter(table -> table.getGame().getId().getId().equals(GWTProvider.ID))
//                .limit(100)
                .forEach(table -> {
                    System.out.println(table.getId());

                    var movesPerPlayer = table.getLog().stream()
                            .filter(logEntry -> logEntry.getType() == LogEntry.Type.IN_GAME_EVENT)
                            .sorted(Comparator.comparing(LogEntry::getTimestamp))
                            .collect(Collectors.groupingBy(LogEntry::getPlayerId));

                    movesPerPlayer.forEach((playerId, logEntries) -> {
                        var state = (GWT) table.getState();

                        state.getPlayerByName(playerId.getId())
                                .map(state::playerState)
                                .ifPresent(playerState -> {

                                    var turns = 0;
                                    var bidding = false;
                                    for (var logEntry : logEntries) {
                                        if (logEntry.getParameters().get(0).equals("BEGIN_TURN") & !bidding) {
                                            turns++;
                                        } else if (logEntry.getParameters().get(0).equals("ACTION")
                                                && (logEntry.getParameters().get(1).equals("PLACE_BID"))) {
                                            turns = 0;
                                            bidding = true;
                                        } else if (logEntry.getParameters().get(0).equals("PLAYER_ORDER")) {
                                            bidding = false;
                                        }
                                    }

                                    playerState.setTurns(turns);

                                    var moves = logEntries.stream()
                                            .filter(logEntry -> logEntry.getParameters().get(0).equals("ACTION"))
                                            .filter(logEntry -> logEntry.getParameters().get(1).equals("MOVE")
                                                    || logEntry.getParameters().get(1).equals("MOVE_TO_PLAYER_BUILDING")
                                                    || logEntry.getParameters().get(1).equals("MOVE_TO_BUILDING"))
                                            .collect(Collectors.toList());

                                    var stops = moves.stream()
                                            .collect(Collectors.groupingBy(logEntry -> state.getTrail().getLocation(logEntry.getParameters().get(2)),
                                                    Collectors.summingInt(e -> 1)));

                                    playerState.setStops(stops);

//                                    System.out.println("table " + table.getId().getId() + " player " + playerId.getId() + " turns " + turns);
                                });
                    });

                    tables.update(table);
                });
    }

}
