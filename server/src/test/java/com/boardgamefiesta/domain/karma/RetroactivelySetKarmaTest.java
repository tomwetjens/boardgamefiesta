/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.karma;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.LogEntry;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.dynamodb.*;
import com.boardgamefiesta.gwt.GWT2Provider;
import com.boardgamefiesta.gwt.GWTProvider;
import com.boardgamefiesta.istanbul.IstanbulProvider;
import lombok.ToString;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@Disabled
public class RetroactivelySetKarmaTest {

    @Mock
    CDI<Object> cdi;
    @Mock
    BeanManager beanManager;

    @Mock
    DynamoDbConfiguration config;

    DynamoDbClient dynamoDbClient;

    Users users;
    Tables tables;
    Karmas karmas;

    @BeforeEach
    void setUp() {
        lenient().when(cdi.getBeanManager()).thenReturn(beanManager);
        CDI.setCDIProvider(() -> cdi);

        lenient().when(config.getTableName()).thenReturn("boardgamefiesta-prod");
        lenient().when(config.getReadGameIdShards()).thenReturn(2);
        lenient().when(config.getWriteGameIdShards()).thenReturn(2);

        dynamoDbClient = DynamoDbClient.create();
        users = new UserDynamoDbRepositoryV2(dynamoDbClient, config);
        tables = new TableDynamoDbRepositoryV2(new Games(), dynamoDbClient, config);
        karmas = new KarmaDynamoDbRepository(dynamoDbClient, config);
    }

    @Test
    void deleteAllKarma() {
        Chunked.stream(dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(config.getTableName())
                .filterExpression("begins_with(PK,:PK) AND begins_with(SK,:SK)")
                .expressionAttributeValues(Map.of(
                        ":PK", AttributeValue.builder().s("User#").build(),
                        ":SK", AttributeValue.builder().s("Karma#").build()
                ))
                .build())
                .items().stream(), 100)
                .forEach(items -> {
                    dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                            .requestItems(Map.of(config.getTableName(), items
                                    .map(item -> WriteRequest.builder()
                                            .deleteRequest(DeleteRequest.builder()
                                                    .key(Map.of(
                                                            "PK", item.get("PK"),
                                                            "SK", item.get("SK")
                                                    ))
                                                    .build())
                                            .build())
                                    .collect(Collectors.toList())))
                            .build());
                });
    }

    @Test
    void listTables() {
        // Assumption: abandoned tables are probably already gone from the DB, so no need to check those
        var min = Instant.parse("2020-06-01T00:00:00.000Z");
        var period = Duration.ofDays(7);
        var gameIds = Stream.of(GWTProvider.ID, IstanbulProvider.ID, GWT2Provider.ID).map(Game.Id::fromString).collect(Collectors.toList());
        Stream.iterate(min, ts -> ts.isBefore(Instant.now()), ts -> ts.plus(period))
                .forEach(from -> {
                    var to = from.plus(period);
                    gameIds.stream()
                            .flatMap(gameId -> {
                                System.out.println("findEndedWithHumanPlayers: " + gameId + " " + from + " -> " + to);
                                return tables.findEndedWithHumanPlayers(gameId, 999999, from, to, true)
                                        .flatMap(table -> table.getPlayers().stream()
                                                .filter(Player::isUser)
                                                .flatMap(player -> {
                                                    var left = player.getStatus() == Player.Status.LEFT ? table.getLog().stream()
                                                            .filter(logEntry -> logEntry.getType() == LogEntry.Type.LEFT)
                                                            .filter(logEntry -> player.getUserId().get().equals(logEntry.getUserId().get()))
                                                            .findFirst()
                                                            .map(logEntry -> new KarmaEvent(logEntry.getUserId().get(), logEntry.getTimestamp(), Karma.Event.LEFT, karma -> karma.left(logEntry.getTimestamp(), table.getId())))
                                                            : Optional.<KarmaEvent>empty();

                                                    var kicked = player.getStatus() == Player.Status.LEFT ? table.getLog().stream()
                                                            .filter(logEntry -> logEntry.getType() == LogEntry.Type.KICK)
                                                            .filter(logEntry -> player.getUserId().get().equals(logEntry.getUserId().get()))
                                                            .findFirst()
                                                            .map(logEntry -> new KarmaEvent(logEntry.getUserId().get(), logEntry.getTimestamp(), Karma.Event.KICKED, karma -> karma.left(logEntry.getTimestamp(), table.getId())))
                                                            : Optional.<KarmaEvent>empty();

                                                    var forceEndTurns = player.getForceEndTurns() > 0 ? table.getLog().stream()
                                                            .filter(logEntry -> logEntry.getType() == LogEntry.Type.FORCE_END_TURN)
                                                            .filter(logEntry -> player.getUserId().get().equals(User.Id.fromString(logEntry.getParameters().get(0))))
                                                            .map(logEntry -> new KarmaEvent(User.Id.fromString(logEntry.getParameters().get(0)), logEntry.getTimestamp(), Karma.Event.FORCE_END_TURN, karma -> karma.forcedEndTurn(logEntry.getTimestamp(), table.getId())))
                                                            : Stream.<KarmaEvent>empty();

                                                    var finishGame = table.getStatus() == Table.Status.ENDED && player.getStatus() != Player.Status.LEFT
                                                            ? Stream.of(new KarmaEvent(player.getUserId().get(), table.getEnded(), Karma.Event.FINISH_GAME, karma -> karma.finishedGame(table.getEnded(), table.getId())))
                                                            : Stream.<KarmaEvent>empty();
                                                    return Stream.of(
                                                            left.stream(),
                                                            kicked.stream(),
                                                            forceEndTurns,
                                                            finishGame
                                                    ).flatMap(Function.identity());
                                                }));
                            })
                            .sorted(Comparator.comparing(KarmaEvent::getTimestamp))
                            .peek(System.out::println)
                            .forEach(karmaEvent -> karmas.add(karmaEvent.getFn().apply(karmas.current(karmaEvent.getUserId(), karmaEvent.getTimestamp().minusSeconds(1)))));
                });
    }

    @Value
    @ToString(exclude = {"fn"})
    private static class KarmaEvent {
        User.Id userId;
        Instant timestamp;
        Karma.Event event;
        Function<Karma, Karma> fn;
    }

}
