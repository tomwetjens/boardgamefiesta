package com.boardgamefiesta.server.query;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.rating.RatingAdjuster;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.LogEntry;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.RatingDynamoDbRepository;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepository;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepository;
import com.boardgamefiesta.gwt.GWT;
import com.boardgamefiesta.gwt.logic.Game;
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
public class AddTurnsMigration {

    @Mock
    DynamoDbConfiguration config;

    Users users;
    Ratings ratings;
    Tables tables;
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
        tables.findAll(com.boardgamefiesta.domain.game.Game.Id.of(GWT.ID), 9999999)
                .filter(table -> table.getGame().getId().getId().equals(GWT.ID))
//                .limit(100)
                .forEach(table -> {
                    System.out.println(table.getId());

                    var movesPerPlayer = table.getLog().stream()
                            .filter(logEntry -> logEntry.getType() == LogEntry.Type.IN_GAME_EVENT)
                            .sorted(Comparator.comparing(LogEntry::getTimestamp))
                            .collect(Collectors.groupingBy(LogEntry::getPlayerId));

                    movesPerPlayer.forEach((playerId, logEntries) -> {
                        var state = (Game) table.getState();

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

//                                    playerState.setTurns(turns);

                                    var moves = logEntries.stream()
                                            .filter(logEntry -> logEntry.getParameters().get(0).equals("ACTION"))
                                            .filter(logEntry -> logEntry.getParameters().get(1).equals("MOVE")
                                                    || logEntry.getParameters().get(1).equals("MOVE_TO_PLAYER_BUILDING")
                                                    || logEntry.getParameters().get(1).equals("MOVE_TO_BUILDING"))
                                            .collect(Collectors.toList());

                                    var stops = moves.stream()
                                            .collect(Collectors.groupingBy(logEntry -> state.getTrail().getLocation(logEntry.getParameters().get(2)),
                                                    Collectors.summingInt(e -> 1)));

//                                    playerState.setStops(stops);
                                });
                    });

                    tables.update(table);
                });
    }

}
