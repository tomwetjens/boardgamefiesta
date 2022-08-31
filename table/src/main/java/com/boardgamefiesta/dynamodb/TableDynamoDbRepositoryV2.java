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

package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.*;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.dynamodb.json.DynamoDbJson;
import com.boardgamefiesta.dynamodb.json.DynamoDbJsonGenerator;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApplicationScoped
@Slf4j
public class TableDynamoDbRepositoryV2 implements Tables {

    private static final String PK = "PK";
    private static final String SK = "SK";

    private static final String GSI1 = "GSI1";
    private static final String GSI1PK = "GSI1PK";
    private static final String GSI1SK = "GSI1SK";
    private static final String GSI2 = "GSI2";
    private static final String GSI2PK = "GSI2PK";
    private static final String GSI2SK = "GSI2SK";
    private static final String GSI3 = "GSI3";
    private static final String GSI3PK = "GSI3PK";
    private static final String GSI3SK = "GSI3SK";
    private static final String GSI4 = "GSI4";
    private static final String GSI4PK = "GSI4PK";
    private static final String GSI4SK = "GSI4SK";

    private static final String TABLE_PREFIX = "Table#";
    private static final String USER_PREFIX = "User#";
    private static final String PLAYER_PREFIX = "Player#";
    private static final String STATE_PREFIX = "State#";
    private static final String GAME_PREFIX = "Game#";
    private static final String LOG_PREFIX = "Log#";

    private static final String VERSION = "Version";
    private static final String TTL = "TTL";

    private static final int MAX_BATCH_WRITE_SIZE = 25;
    private static final int MAX_BATCH_GET_ITEM_SIZE = 100;
    private static final Table.Id MAX_TABLE_ID = Table.Id.of("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final DateTimeFormatter TIMESTAMP_SECS_FORMATTER = new DateTimeFormatterBuilder()
            .parseStrict()
            .appendInstant(0) // No fractional second
            .toFormatter();
    private static final DateTimeFormatter TIMESTAMP_MILLIS_FORMATTER = new DateTimeFormatterBuilder()
            .parseStrict()
            .appendInstant(3) // 3 digits fractional second
            .toFormatter();

    private final Games games;
    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public TableDynamoDbRepositoryV2(@NonNull Games games,
                                     @NonNull DynamoDbClient client,
                                     @NonNull DynamoDbConfiguration config) {
        this.games = games;
        this.client = client;
        this.config = config;
    }

    private String shardedGameGSIPK(Game.Id gameId, Table.Id tableId) {
        return GAME_PREFIX + gameId.getId() + "#" + Math.abs(tableId.hashCode()) % config.writeGameIdShards();
    }

    @Override
    public Optional<Table> findById(Table.Id id) {
        log.debug("findById: {}", id);

        // Query both the Table#<Id> + latest State#<timestamp> in one go,
        // because it is always needed by caller
        var response = client.query(QueryRequest.builder()
                .tableName(config.tableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + ">:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(TABLE_PREFIX + id.getId()),
                        ":SK", Item.s(STATE_PREFIX)
                ))
                .scanIndexForward(false)
                .limit(2)
                .build());

        if (response.hasItems() && !response.items().isEmpty()
                && response.items().get(0).get(SK).s().startsWith(TABLE_PREFIX)) {
            return Optional.of(mapToTable(response.items().size() > 1 && response.items().get(1).get(SK).s().startsWith(STATE_PREFIX)
                    // Latest State# found
                    ? List.of(Item.of(response.items().get(0)), Item.of(response.items().get(1)))
                    // No State# found
                    : List.of(Item.of(response.items().get(0)))));
        }

        return Optional.empty();
    }

    @Override
    public void add(Table table) {
        if (countActive(table.getOwnerId()) >= MAX_ACTIVE_GAMES) {
            throw new ExceedsMaxActiveGames();
        }

        put(table);
    }

    public void put(Table table) {
        Stream.of(
                Stream.of(WriteRequest.builder()
                        .putRequest(PutRequest.builder()
                                .item(mapItemFromTable(table).asMap())
                                .build())
                        .build()),

                table.getPlayers().stream()
                        // Adjacency items only needed for human players,
                        // since computers will never query the index
                        .filter(Player::isUser)
                        .map(player -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(mapItemFromPlayer(player, table))
                                        .build())
                                .build()),

                table.getCurrentState().get().stream()
                        .map(currentState -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(mapItemFromState(table.getId(),
                                                table.getGame(), currentState.getState(), currentState.getTimestamp(),
                                                currentState.getPrevious().get().map(Table.HistoricState::getTimestamp)))
                                        .build())
                                .build()),

                table.getLog().stream()
                        .map(logEntry -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(mapItemFromLogEntry(logEntry, table.getId()).asMap())
                                        .build())
                                .build())
        )
                .flatMap(Function.identity())
                .collect(Chunked.chunked(MAX_BATCH_WRITE_SIZE))
                .forEach(chunk ->
                        client.batchWriteItem(BatchWriteItemRequest.builder()
                                .requestItems(Map.of(config.tableName(), chunk))
                                .build()));
    }

    private Item mapItemFromLogEntry(LogEntry logEntry, Table.Id tableId) {
        return new Item()
                .setString(PK, TABLE_PREFIX + tableId.getId())
                .setString(SK, LOG_PREFIX + TIMESTAMP_MILLIS_FORMATTER.format(logEntry.getTimestamp()))
                .setString("UserId", logEntry.getUserId().map(User.Id::getId).orElse(null))
                .setString("PlayerId", logEntry.getPlayerId().getId())
                .setEnum("Type", logEntry.getType())
                .set("Parameters", AttributeValue.builder()
                        .l(logEntry.getParameters().stream()
                                .map(Item::s)
                                .collect(Collectors.toList()))
                        .build());
    }

    @Override
    public void update(Table table) {
        if (table.getCurrentState().isResolved()) {
            table.getCurrentState().get()
                    .filter(Table.CurrentState::isChanged)
                    .ifPresent(currentState -> addState(table.getId(),
                            table.getGame(), currentState.getState(), currentState.getTimestamp(),
                            currentState.getPrevious().get().map(Table.HistoricState::getTimestamp)));
        }

        var updateItem = new UpdateItem()
                .setInt(VERSION, table.getVersion() + 1)
                .setEnum("Type", table.getType())
                .setEnum("Mode", table.getMode())
                .setEnum("Visibility", table.getVisibility())
                .setEnum("Status", table.getStatus())
                .setInt("Progress", table.getProgress())
                .setInstant("Updated", table.getUpdated())
                .setTTL(TTL, table.getExpires().orElse(null))
                .setString("OwnerId", table.getOwnerId().getId())
                .set("Players", AttributeValue.builder().l(table.getPlayers().stream()
                        .map(this::mapFromPlayer)
                        .collect(Collectors.toList()))
                        .build())
                .set("Seats", AttributeValue.builder()
                        .l(table.getSeats().stream()
                                .map(this::mapFromSeat)
                                .collect(Collectors.toList()))
                        .build())
                .set("Options", mapFromOptions(table.getOptions()))
                .setInt("MinNumberOfPlayers", table.getMinNumberOfPlayers())
                .setInt("MaxNumberOfPlayers", table.getMaxNumberOfPlayers())
                .setBoolean("AutoStart", table.isAutoStart());

        if (table.getStarted() != null) {
            updateItem.setInstant("Started", table.getStarted());
        }

        if (table.getEnded() != null) {
            updateItem.setInstant("Ended", table.getEnded());
        }

        if (table.hasMoreThanOneHumanPlayer()) {
            if (table.getStatus() == Table.Status.ENDED) {
                updateItem.setString(GSI1PK, shardedGameGSIPK(table.getGame().getId(), table.getId()));
                updateItem.setString(GSI1SK, GSISK.fromTable(table));
            } else {
                updateItem.remove(GSI1PK, GSI1SK);
            }

            if (table.getStatus() == Table.Status.STARTED) {
                updateItem.setString(GSI2PK, shardedGameGSIPK(table.getGame().getId(), table.getId()));
                updateItem.setString(GSI2SK, GSISK.fromTable(table));
            } else {
                updateItem.remove(GSI2PK, GSI2SK);
            }
        } else {
            updateItem.remove(GSI1PK, GSI1SK, GSI2PK, GSI2SK);
        }

        if (table.getStatus() == Table.Status.NEW && table.canJoin()) {
            updateItem.setString(GSI3PK, shardedGameGSIPK(table.getGame().getId(), table.getId()));
            updateItem.setString(GSI3SK, GSISK.fromTable(table));
        } else {
            updateItem.remove(GSI3PK, GSI3SK);
        }

        updateItem.expressionAttributeValue(":ExpectedVersion", Item.n(table.getVersion()));

        var request = UpdateItemRequest.builder()
                .tableName(config.tableName())
                .key(Map.of(
                        PK, Item.s(TABLE_PREFIX + table.getId().getId()),
                        SK, Item.s(TABLE_PREFIX + table.getId().getId())
                ))
                .conditionExpression(VERSION + "=:ExpectedVersion")
                .updateExpression(updateItem.getUpdateExpression())
                .expressionAttributeNames(updateItem.getExpressionAttributeNames())
                .expressionAttributeValues(updateItem.getExpressionAttributeValues())
                .build();

        try {
            client.updateItem(request);
        } catch (ConditionalCheckFailedException e) {
            throw new Repository.ConcurrentModificationException(e);
        }

        updatePlayerItems(table);

        addLogEntries(table);
    }

    private void addState(@NonNull Table.Id tableId,
                          @NonNull Game game, @NonNull State state, @NonNull Instant timestamp,
                          @NonNull Optional<Instant> previousTimestamp) {
        client.putItem(PutItemRequest.builder()
                .tableName(config.tableName())
                .item(mapItemFromState(tableId, game, state, timestamp, previousTimestamp))
                .build());
    }

    private void addLogEntries(Table table) {
        log.debug("addLogEntries: {}", table.getId());

        var log = table.getLog();

        var logEntries = log instanceof LazyLog
                ? ((LazyLog) log).pending()
                : log.stream();

        logEntries
                .map(logEntry -> mapItemFromLogEntry(logEntry, table.getId()))
                .map(logItem -> WriteRequest.builder()
                        .putRequest(PutRequest.builder()
                                .item(logItem.asMap())
                                .build())
                        .build())
                .collect(Chunked.chunked(MAX_BATCH_WRITE_SIZE))
                .forEach(writeRequests ->
                        client.batchWriteItem(BatchWriteItemRequest.builder()
                                .requestItems(Map.of(config.tableName(), writeRequests))
                                .build()));
    }

    private int countActive(User.Id userId) {
        return client.query(QueryRequest.builder()
                .tableName(config.tableName())
                .indexName(GSI2)
                .keyConditionExpression("GSI2PK=:GSI2PK AND begins_with(GSI2SK, :GSI2SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI2PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI2SK", Item.s(TABLE_PREFIX)
                ))
                .select(Select.COUNT)
                .build())
                .count();
    }

    @Override
    public Stream<Table> findActive(User.Id userId) {
        return findByIds(client.queryPaginator(QueryRequest.builder()
                .tableName(config.tableName())
                .indexName(GSI2)
                .scanIndexForward(false)
                .keyConditionExpression(GSI2PK + "=:GSI2PK AND begins_with(" + GSI2SK + ",:GSI2SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI2PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI2SK", Item.s(TABLE_PREFIX)
                ))
                .build())
                .items().stream()
                .map(item -> GSISK.parse(item.get(GSI2SK).s()))
                .map(GSISK::getTableId));
    }

    @Override
    public Stream<Table> findAll(User.@NonNull Id userId, int maxResults) {
        return findByIds(client.queryPaginator(QueryRequest.builder()
                .tableName(config.tableName())
                .indexName(GSI1)
                .scanIndexForward(false)
                .keyConditionExpression(GSI1PK + "=:GSI1PK AND begins_with(" + GSI1SK + ",:GSI1SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI1PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI1SK", Item.s(TABLE_PREFIX)
                ))
                .build())
                .items().stream()
                .map(item -> GSISK.parse(item.get(GSI1SK).s()))
                .map(GSISK::getTableId)
                .limit(maxResults));
    }

    @Override
    public Stream<Table> findAll(@NonNull User.Id userId, Game.Id gameId, int maxResults) {
        return findByIds(client.queryPaginator(QueryRequest.builder()
                .tableName(config.tableName())
                .indexName(GSI1)
                .scanIndexForward(false)
                .keyConditionExpression(GSI1PK + "=:GSI1PK AND begins_with(" + GSI1SK + ",:GSI1SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI1PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI1SK", Item.s(TABLE_PREFIX)
                ))
                .build())
                .items().stream()
                .map(item -> GSISK.parse(item.get(GSI1SK).s()))
                .filter(sk -> gameId.equals(sk.getGameId()))
                .map(GSISK::getTableId)
                .limit(maxResults));
    }

    @Override
    public Stream<Table> findStarted(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from,
                                     @NonNull Instant to) {
        return findStarted(gameId, maxResults, from, to, MAX_TABLE_ID);
    }

    @Override
    public Stream<Table> findStarted(@NonNull Game.Id gameId, int maxResults,
                                     @NonNull Instant from,
                                     @NonNull Instant to,
                                     @NonNull Table.Id lastEvaluatedId) {
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be >=1, but was: " + maxResults);
        }
        if (from.isBefore(MIN_TIMESTAMP)) {
            throw new IllegalArgumentException("'From' must not be before " + MIN_TIMESTAMP + ", but was: " + from);
        }
        if (to.isAfter(MAX_TIMESTAMP)) {
            throw new IllegalArgumentException("'To' must not be after " + MAX_TIMESTAMP + ", but was: " + to);
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'To' must be after " + from + ", but was: " + to);
        }

        return findByIds(IntStream.range(0, config.readGameIdShards())
                // Scatter
                .parallel()
                .mapToObj(shard -> {
                    var gsi2skTo = GSISK.from(lastEvaluatedId, Table.Status.STARTED, to, gameId);
                    return client.queryPaginator(QueryRequest.builder()
                            .tableName(config.tableName())
                            .indexName(GSI2)
                            .scanIndexForward(false)
                            .keyConditionExpression(GSI2PK + "=:GSI2PK AND " + GSI2SK + " BETWEEN :GSI2SKFrom AND :GSI2SKTo")
                            .expressionAttributeValues(Map.of(
                                    ":GSI2PK", Item.s(GAME_PREFIX + gameId.getId() + "#" + shard),
                                    ":GSI2SKFrom", Item.s(GSISK.partial(Table.Status.STARTED, from)),
                                    ":GSI2SKTo", Item.s(gsi2skTo)
                            ))
                            .limit(maxResults + 1) // + 1 because BETWEEN is inclusive, filter out later
                            .build())
                            .stream()
                            .filter(QueryResponse::hasItems)
                            .flatMap(response -> response.items().stream())
                            .filter(item -> item.get(GSI2SK).s().compareTo(gsi2skTo) < 0) // Make upper limit exclusive, because BETWEEN is inclusive
                            .limit(maxResults);
                })
                // Gather
                .flatMap(Function.identity())
                .sorted(Comparator.<Map<String, AttributeValue>, String>comparing(item -> item.get(GSI2SK).s()).reversed())
                .map(item -> Table.Id.of(item.get(PK).s().replace(TABLE_PREFIX, "")))
                .limit(maxResults));
    }

    @Override
    public Stream<Table> findOpen(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from,
                                  @NonNull Instant to) {
        return findOpen(gameId, maxResults, from, to, MAX_TABLE_ID);
    }

    @Override
    public Stream<Table> findOpen(@NonNull Game.Id gameId, int maxResults,
                                  @NonNull Instant from,
                                  @NonNull Instant to,
                                  @NonNull Table.Id lastEvaluatedId) {
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be >=1, but was: " + maxResults);
        }
        if (from.isBefore(MIN_TIMESTAMP)) {
            throw new IllegalArgumentException("'From' must not be before " + MIN_TIMESTAMP + ", but was: " + from);
        }
        if (to.isAfter(MAX_TIMESTAMP)) {
            throw new IllegalArgumentException("'To' must not be after " + MAX_TIMESTAMP + ", but was: " + to);
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'To' must be after " + from + ", but was: " + to);
        }

        return findByIds(IntStream.range(0, config.readGameIdShards())
                // Scatter
                .parallel()
                .mapToObj(shard -> {
                    var gsi3skTo = GSISK.from(lastEvaluatedId, Table.Status.NEW, to, gameId);
                    return client.queryPaginator(QueryRequest.builder()
                            .tableName(config.tableName())
                            .indexName(GSI3)
                            .scanIndexForward(false)
                            .keyConditionExpression(GSI3PK + "=:GSI3PK AND " + GSI3SK + " BETWEEN :GSI3SKFrom AND :GSI3SKTo")
                            .expressionAttributeValues(Map.of(
                                    ":GSI3PK", Item.s(GAME_PREFIX + gameId.getId() + "#" + shard),
                                    ":GSI3SKFrom", Item.s(GSISK.partial(Table.Status.NEW, from)),
                                    ":GSI3SKTo", Item.s(gsi3skTo)
                            ))
                            .limit(maxResults + 1) // + 1 because BETWEEN is inclusive, filter out later
                            .build())
                            .stream()
                            .filter(QueryResponse::hasItems)
                            .flatMap(response -> response.items().stream())
                            .filter(item -> item.get(GSI3SK).s().compareTo(gsi3skTo) < 0) // Make upper limit exclusive, because BETWEEN is inclusive
                            .limit(maxResults);
                })
                // Gather
                .flatMap(Function.identity())
                .sorted(Comparator.<Map<String, AttributeValue>, String>comparing(item -> item.get(GSI3SK).s()).reversed())
                .map(item -> Table.Id.of(item.get(PK).s().replace(TABLE_PREFIX, "")))
                .limit(maxResults));
    }

    @Override
    public Stream<Table> findEndedWithHumanPlayers(@NonNull Game.Id gameId, int maxResults, @NonNull Instant from, @NonNull Instant to, boolean ascending) {
        return findEndedWithHumanPlayers(gameId, maxResults, from, to, ascending, MAX_TABLE_ID);
    }

    @Override
    public Stream<Table> findEndedWithHumanPlayers(@NonNull Game.Id gameId, int maxResults,
                                                   @NonNull Instant from,
                                                   @NonNull Instant to,
                                                   boolean ascending,
                                                   @NonNull Table.Id lastEvaluatedId) {
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be >=1, but was: " + maxResults);
        }
        if (from.isBefore(MIN_TIMESTAMP)) {
            throw new IllegalArgumentException("'From' must not be before " + MIN_TIMESTAMP + ", but was: " + from);
        }
        if (to.isAfter(MAX_TIMESTAMP)) {
            throw new IllegalArgumentException("'To' must not be after " + MAX_TIMESTAMP + ", but was: " + to);
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'To' must be after " + from + ", but was: " + to);
        }

        return findByIds(IntStream.range(0, config.readGameIdShards())
                // Scatter
                .parallel()
                .mapToObj(shard -> {
                    var gsi1skTo = GSISK.from(lastEvaluatedId, Table.Status.ENDED, to, gameId);
                    return client.queryPaginator(QueryRequest.builder()
                            .tableName(config.tableName())
                            .indexName(GSI1)
                            .scanIndexForward(ascending)
                            .keyConditionExpression(GSI1PK + "=:GSI1PK AND " + GSI1SK + " BETWEEN :GSI1SKFrom AND :GSI1SKTo")
                            .expressionAttributeValues(Map.of(
                                    ":GSI1PK", Item.s(GAME_PREFIX + gameId.getId() + "#" + shard),
                                    ":GSI1SKFrom", Item.s(GSISK.partial(Table.Status.ENDED, from)),
                                    ":GSI1SKTo", Item.s(gsi1skTo)
                            ))
                            .limit(maxResults + 1) // + 1 because BETWEEN is inclusive, filter out later
                            .build())
                            .stream()
                            .filter(QueryResponse::hasItems)
                            .flatMap(response -> response.items().stream())
                            .filter(item -> item.get(GSI1SK).s().compareTo(gsi1skTo) < 0) // Make upper limit exclusive, because BETWEEN is inclusive
                            .limit(maxResults);
                })
                // Gather
                .flatMap(Function.identity())
                .sorted(ascending
                        ? Comparator.<Map<String, AttributeValue>, String>comparing(item -> item.get(GSI1SK).s())
                        : Comparator.<Map<String, AttributeValue>, String>comparing(item -> item.get(GSI1SK).s()).reversed())
                .map(item -> Table.Id.of(item.get(PK).s().replace(TABLE_PREFIX, "")))
                .limit(maxResults));
    }

    /**
     * @return guarantees same order as input
     */
    private Stream<Table> findByIds(Stream<Table.Id> ids) {
        return Chunked.stream(ids, MAX_BATCH_GET_ITEM_SIZE)
                .map(chunk -> chunk
                        .map(id -> Map.of(
                                PK, Item.s(TABLE_PREFIX + id.getId()),
                                SK, Item.s(TABLE_PREFIX + id.getId())))
                        .collect(Collectors.toList()))
                .flatMap(keys -> {
                    var response = client.batchGetItem(BatchGetItemRequest.builder()
                            .requestItems(Map.of(config.tableName(),
                                    KeysAndAttributes.builder()
                                            .keys(keys)
                                            .build()))
                            .build());

                    if (response.hasResponses()) {
                        var items = response.responses().get(config.tableName()).stream()
                                .collect(Collectors.toMap(item -> item.get(PK).s(), Function.identity()));
                        return keys.stream()
                                .map(key -> key.get(PK).s())
                                .map(items::get)
                                .map(Item::of)
                                .map(Collections::singletonList)
                                .map(this::mapToTable);
                    }
                    return Stream.empty();
                });
    }

    private void updatePlayerItems(Table table) {
        var trackingSet = (TrackingSet<Player>) table.getPlayers();

        if (!trackingSet.getAdded().isEmpty() || !trackingSet.getRemoved().isEmpty()) {
            var writeRequests = Stream.concat(
                    trackingSet.getRemoved().stream()
                            .filter(Player::isUser)
                            .map(player -> WriteRequest.builder()
                                    .deleteRequest(DeleteRequest.builder()
                                            .key(Map.of(
                                                    PK, Item.s(TABLE_PREFIX + table.getId().getId()),
                                                    SK, Item.s(PLAYER_PREFIX + player.getId().getId())
                                            ))
                                            .build())
                                    .build()),
                    trackingSet.getAdded().stream()
                            .filter(Player::isUser)
                            .map(player -> WriteRequest.builder()
                                    .putRequest(PutRequest.builder()
                                            .item(mapItemFromPlayer(player, table))
                                            .build())
                                    .build()))
                    .collect(Collectors.toList());

            if (!writeRequests.isEmpty()) {
                client.batchWriteItem(BatchWriteItemRequest.builder()
                        .requestItems(Map.of(config.tableName(), writeRequests))
                        .build());
            }
        }

        trackingSet.getNotAddedOrRemoved()
                .filter(Player::isUser)
                .forEach(player -> updatePlayerItem(player, table));

        trackingSet.flush();
    }

    private void updatePlayerItem(Player player, Table table) {
        var updateItem = new UpdateItem();

        updateItem.setTTL(TTL, table.getExpires().orElse(null));

        player.getUserId().ifPresentOrElse(userId -> {
            updateItem.setString(GSI1SK, GSISK.fromTable(table));

            if (player.isActive() && table.isActive()) {
                updateItem.setString(GSI2SK, GSISK.fromTable(table));
            } else {
                updateItem.remove(GSI2PK, GSI2SK);
            }
        }, () -> updateItem.remove(GSI1PK, GSI1SK, GSI2PK, GSI2SK));

        client.updateItem(UpdateItemRequest.builder()
                .tableName(config.tableName())
                .key(Map.of(
                        PK, Item.s(TABLE_PREFIX + table.getId().getId()),
                        SK, Item.s(PLAYER_PREFIX + player.getId().getId())))
                .updateExpression(updateItem.getUpdateExpression())
                .expressionAttributeNames(updateItem.getExpressionAttributeNames())
                .expressionAttributeValues(updateItem.getExpressionAttributeValues())
                .build());
    }

    private Item mapItemFromTable(Table table) {
        var item = new Item()
                .setString(PK, TABLE_PREFIX + table.getId().getId())
                .setString(SK, TABLE_PREFIX + table.getId().getId())
                .setInt(VERSION, table.getVersion());

        if (table.hasMoreThanOneHumanPlayer()) {
            if (table.getStatus() == Table.Status.ENDED) {
                item.setString(GSI1PK, shardedGameGSIPK(table.getGame().getId(), table.getId()));
                item.setString(GSI1SK, GSISK.fromTable(table));
            }

            if (table.getStatus() == Table.Status.STARTED) {
                item.setString(GSI2PK, shardedGameGSIPK(table.getGame().getId(), table.getId()));
                item.setString(GSI2SK, GSISK.fromTable(table));
            }
        }

        if (table.getStatus() == Table.Status.NEW && table.canJoin()) {
            item.setString(GSI3PK, shardedGameGSIPK(table.getGame().getId(), table.getId()));
            item.setString(GSI3SK, GSISK.fromTable(table));
        }

        item.setString(GSI4PK, shardedGameGSIPK(table.getGame().getId(), table.getId()));
        item.setString(GSI4SK, GSISK.fromTable(table));

        item.setString("GameId", table.getGame().getId().getId())
                .setEnum("Type", table.getType())
                .setEnum("Mode", table.getMode())
                .setEnum("Visibility", table.getVisibility())
                .setEnum("Status", table.getStatus())
                .setInt("Progress", table.getProgress())
                .set("Options", mapFromOptions(table.getOptions()))
                .setInstant("Created", table.getCreated())
                .setInstant("Updated", table.getUpdated())
                .setInt("MinNumberOfPlayers", table.getMinNumberOfPlayers())
                .setInt("MaxNumberOfPlayers", table.getMaxNumberOfPlayers())
                .setBoolean("AutoStart", table.isAutoStart());
        if (table.getStarted() != null) {
            item.setInstant("Started", table.getStarted());
        }
        if (table.getEnded() != null) {
            item.setInstant("Ended", table.getEnded());
        }
        item.setTTL(TTL, table.getExpires().orElse(null))
                .setString("OwnerId", table.getOwnerId().getId())
                .set("Players", AttributeValue.builder()
                        .l(table.getPlayers().stream()
                                .map(this::mapFromPlayer)
                                .collect(Collectors.toList()))
                        .build())
                .set("Seats", AttributeValue.builder()
                        .l(table.getSeats().stream()
                                .map(this::mapFromSeat)
                                .collect(Collectors.toList()))
                        .build());

        return item;
    }

    private Map<String, AttributeValue> mapItemFromPlayer(Player player, Table table) {
        var item = new Item();

        item.setString(PK, TABLE_PREFIX + table.getId().getId());
        item.setString(SK, PLAYER_PREFIX + player.getId().getId());

        player.getUserId().ifPresent(userId -> {
            item.setString(GSI1PK, USER_PREFIX + userId.getId());
            item.setString(GSI1SK, GSISK.fromTable(table));

            if (table.isActive()) {
                item.setString(GSI2PK, USER_PREFIX + userId.getId());
                item.setString(GSI2SK, GSISK.fromTable(table));
            }
        });

        item.setTTL(TTL, table.getExpires().orElse(null));

        return item.asMap();
    }

    private Map<String, AttributeValue> mapItemFromState(Table.Id tableId,
                                                         Game game, State state, Instant timestamp,
                                                         Optional<Instant> previousTimestamp) {
        var stateSerializer = game.getProvider().getStateSerializer();

        AttributeValue serialized;
        if (stateSerializer.isJsonGeneratorSupported()) {
            var jsonGenerator = new DynamoDbJsonGenerator();
            stateSerializer.serialize(state, jsonGenerator);
            serialized = jsonGenerator.getAttributeValue();
        } else {
            serialized = DynamoDbJson.toJson(jsonBuilderFactory -> stateSerializer.serialize(state, jsonBuilderFactory));
        }

        return mapItemFromState(tableId, timestamp, previousTimestamp, serialized);
    }

    private static Map<String, AttributeValue> mapItemFromState(Table.Id tableId,
                                                                Instant timestamp,
                                                                Optional<Instant> previousTimestamp,
                                                                AttributeValue state) {
        var item = new HashMap<String, AttributeValue>();
        item.put(PK, Item.s(TABLE_PREFIX + tableId.getId()));
        item.put(SK, Item.s(STATE_PREFIX + TIMESTAMP_MILLIS_FORMATTER.format(timestamp)));
        item.put("Timestamp", Item.s(timestamp));

        previousTimestamp.map(Item::s).ifPresent(p -> item.put("Previous", p));

        item.put("State", state);
        return item;
    }

    private AttributeValue mapFromPlayer(Player player) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Id", Item.s(player.getId().getId()));
        map.put("Type", Item.s(player.getType().name()));
        map.put("UserId", player.getUserId().map(userId -> Item.s(userId.getId())).orElse(null));
        map.put("Status", Item.s(player.getStatus().name()));
        map.put("Color", player.getColor().map(Item::s).orElse(null));
        map.put("Score", player.getScore().map(score -> AttributeValue.builder().n(Integer.toString(score)).build()).orElse(null));
        map.put("Winner", player.getWinner().map(winner -> AttributeValue.builder().bool(winner).build()).orElse(null));
        map.put("Created", Item.s(player.getCreated()));
        map.put("Updated", Item.s(player.getUpdated()));
        map.put("Turn", AttributeValue.builder().bool(player.isTurn()).build());
        map.put("TurnLimit", player.getTurnLimit().map(Item::s).orElse(null));
        map.put("ForceEndTurns", AttributeValue.builder().n(Integer.toString(player.getForceEndTurns())).build());
        return AttributeValue.builder().m(map).build();
    }

    private AttributeValue mapFromSeat(Seat seat) {
        var map = new HashMap<String, AttributeValue>();
        map.put("PlayerId", Item.s(seat.getPlayerId().getId()));
        seat.getPlayerColor().ifPresent(playerColor -> map.put("PlayerColor", Item.s(playerColor)));
        return AttributeValue.builder().m(map).build();
    }

    private AttributeValue mapFromOptions(Options options) {
        return AttributeValue.builder().m(options.asMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue() instanceof Boolean) {
                        return AttributeValue.builder().bool((Boolean) entry.getValue()).build();
                    } else if (entry.getValue() instanceof Number) {
                        return AttributeValue.builder().n(entry.getValue().toString()).build();
                    } else {
                        return Item.s(entry.getValue().toString());
                    }
                })))
                .build();
    }

    private Table mapToTable(List<Item> items) {
        var item = items.get(0);
        if (!item.get(PK).s().startsWith(TABLE_PREFIX) || !item.get(SK).s().equals(item.get(PK).s())) {
            throw new IllegalArgumentException("Not a valid Table item: PK=" + item.get(PK).s() + " SK=" + item.get(SK).s());
        }

        var id = Table.Id.of(item.getString(PK).replace(TABLE_PREFIX, ""));

        var players = item.get("Players").l().stream()
                .map(this::mapToPlayer)
                .collect(Collectors.toCollection(TrackingSet::new));
        players.flush(); // Consider everything added up until now to be unchanged

        var playerMap = players.stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        var game = games.get(Game.Id.fromString(item.getString("GameId")));

        return Table.builder()
                .id(id)
                .version(item.getInt(VERSION))
                .type(item.getEnum("Type", Table.Type.class))
                .mode(item.getEnum("Mode", Table.Mode.class))
                .visibility(item.getOptionalEnum("Visibility", Table.Visibility.class).orElse(Table.Visibility.PRIVATE))
                .game(game)
                .status(item.getEnum("Status", Table.Status.class))
                .progress(item.getOptionalInt("Progress").orElse(0))
                .options(mapToOptions(item.get("Options")))
                .created(item.getInstant("Created"))
                .updated(item.getInstant("Updated"))
                .started(item.getOptionalInstant("Started").orElse(null))
                .ended(item.getOptionalInstant("Ended").orElse(null))
                .ownerId(User.Id.of(item.getString("OwnerId")))
                .seats(item.getOptionalNotNull("Seats")
                        .map(seats -> seats.l().stream().map(seat -> mapToSeat(seat, playerMap)))
                        .orElseGet(() -> players.stream().map(Seat::fromPlayer))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .players(players)
                .currentState(items.size() > 1
                        ? Lazy.of(Optional.of(mapToCurrentState(id, items.get(1), game)))
                        : Lazy.defer(() -> getCurrentState(id, game)))
                .historicStates(new Table.HistoricStates() {
                    @Override
                    public Optional<Table.HistoricState> at(Instant timestamp) {
                        var historicState = super.at(timestamp);
                        if (historicState.isEmpty()) {
                            return findHistoricStateAt(id, timestamp, game);
                        }
                        return historicState;
                    }
                })
                .log(new LazyLog((since, before, limit) -> findLogEntries(id, since, before, limit)))
                .minNumberOfPlayers(item.getOptionalInt("MinNumberOfPlayers").orElse(game.getMinNumberOfPlayers()))
                .maxNumberOfPlayers(item.getOptionalInt("MaxNumberOfPlayers").orElse(game.getMaxNumberOfPlayers()))
                .autoStart(item.getOptionalBoolean("AutoStart").orElse(false))
                .build();
    }

    private Table.HistoricState mapToHistoricState(Table.Id tableId, Item item, Game game) {
        return Table.HistoricState.builder()
                .state(mapToState(game, item.get("State")))
                .timestamp(Instant.parse(item.get("Timestamp").s()))
                .previous(Optional.ofNullable(item.get("Previous"))
                        .map(AttributeValue::s)
                        .map(Instant::parse)
                        .map(previous -> Lazy.defer(() -> getHistoricState(tableId, previous, game)))
                        .orElse(Lazy.of(Optional.empty())))
                .build();
    }

    private Table.CurrentState mapToCurrentState(Table.Id tableId, Item item, Game game) {
        if (!item.get(PK).s().equals(TABLE_PREFIX + tableId.getId()) && !item.get(SK).s().startsWith(STATE_PREFIX)) {
            throw new IllegalArgumentException("Not a valid State item: PK=" + item.get(PK).s() + " SK=" + item.get(SK).s());
        }

        return Table.CurrentState.builder()
                .state(mapToState(game, item.get("State")))
                .timestamp(Instant.parse(item.get("Timestamp").s()))
                .previous(Optional.ofNullable(item.get("Previous"))
                        .map(AttributeValue::s)
                        .map(Instant::parse)
                        .map(previous -> Lazy.defer(() -> getHistoricState(tableId, previous, game)))
                        .orElse(Lazy.of(Optional.empty())))
                .changed(false)
                .build();
    }

    private Optional<Table.CurrentState> getCurrentState(Table.Id tableId, Game game) {
        log.debug("getCurrentState: {}", tableId);

        var response = client.query(QueryRequest.builder()
                .tableName(config.tableName())
                .keyConditionExpression("PK=:PK AND begins_with(SK,:SK)")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(TABLE_PREFIX + tableId.getId()),
                        ":SK", Item.s(STATE_PREFIX)
                ))
                .scanIndexForward(false)
                .limit(1) // Latest
                .build());

        if (response.hasItems() && response.count() > 0) {
            return Optional.of(mapToCurrentState(tableId, Item.of(response.items().get(0)), game));
        }
        return Optional.empty();
    }

    private Optional<Table.HistoricState> getHistoricState(Table.Id tableId, Instant timestamp, Game game) {
        log.debug("getHistoricState: {} {}", tableId, timestamp);

        var response = client.getItem(GetItemRequest.builder()
                .tableName(config.tableName())
                .key(Map.of(
                        PK, Item.s(TABLE_PREFIX + tableId.getId()),
                        SK, Item.s(STATE_PREFIX + TIMESTAMP_MILLIS_FORMATTER.format(timestamp))
                ))
                .build());

        if (response.hasItem()) {
            var item = response.item();

            return Optional.of(mapToHistoricState(tableId, Item.of(item), game));
        }

        return Optional.empty();
    }

    private Optional<Table.HistoricState> findHistoricStateAt(Table.Id tableId, Instant timestamp, Game game) {
        log.debug("findHistoricStateAt: {} {}", tableId, timestamp);

        var response = client.query(QueryRequest.builder()
                .tableName(config.tableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + " BETWEEN :SKFrom AND :SKTo")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(TABLE_PREFIX + tableId.getId()),
                        ":SKFrom", Item.s(STATE_PREFIX + TIMESTAMP_MILLIS_FORMATTER.format(MIN_TIMESTAMP)),
                        ":SKTo", Item.s(STATE_PREFIX + TIMESTAMP_MILLIS_FORMATTER.format(timestamp))
                ))
                .scanIndexForward(false)
                .limit(1)
                .build());

        if (response.hasItems()) {
            var items = response.items();

            if (items.size() == 1) {
                var item = items.get(0);

                return Optional.of(mapToHistoricState(tableId, Item.of(item), game));
            }
        }

        return Optional.empty();
    }

    private Seat mapToSeat(AttributeValue attributeValue, Map<Player.Id, Player> playerMap) {
        var item = Item.of(attributeValue.m());

        var playerId = Player.Id.of(item.getString("PlayerId"));

        return Seat.builder()
                .playerId(playerId)
                .playerColor(item.getOptionalEnum("PlayerColor", PlayerColor.class).orElse(null))
                .player(playerMap.get(playerId))
                .build();
    }

    @Override
    public Stream<LogEntry> findLogEntries(@NonNull Table.Id tableId, @NonNull Instant since, @NonNull Instant before, int limit) {
        log.debug("findLogEntries: {} >={} <{} limit {}", tableId, since, before, limit);

        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.tableName())
                // Needs to put an upper limit on SK because else it will also return other items like "Player#..." and "State#..."
                .keyConditionExpression("PK=:PK AND SK BETWEEN :SKFrom AND :SKTo")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(TABLE_PREFIX + tableId.getId()),
                        ":SKFrom", Item.s(LOG_PREFIX + TIMESTAMP_MILLIS_FORMATTER.format(since)),
                        ":SKTo", Item.s(LOG_PREFIX + TIMESTAMP_MILLIS_FORMATTER.format(before))
                ))
                .scanIndexForward(false)
                .limit(Math.min(9999, limit) + 2) // Retrieve 2 more because BETWEEN is inclusive on both ends
                .build())
                .items().stream()
                .map(Item::of)
                .map(this::mapToLogEntry)
                // Filter because BETWEEN is inclusive on both ends
                .filter(logEntry -> logEntry.getTimestamp().isAfter(since) && logEntry.getTimestamp().isBefore(before))
                .limit(limit);
    }

    private LogEntry mapToLogEntry(Item item) {
        return LogEntry.builder()
                .timestamp(Instant.parse(item.getString("SK").replace(LOG_PREFIX, "")))
                .playerId(Player.Id.of(item.getString("PlayerId")))
                .userId(item.getOptionalString("UserId")
                        .map(User.Id::of)
                        .orElse(null))
                .type(item.getEnum("Type", LogEntry.Type.class))
                .parameters(item.get("Parameters").l().stream()
                        .map(AttributeValue::s)
                        .collect(Collectors.toList()))
                .build();
    }

    private State mapToState(Game game, AttributeValue attributeValue) {
        return DynamoDbJson.fromJson(attributeValue, game.getProvider().getStateDeserializer()::deserialize);
    }

    private Options mapToOptions(AttributeValue attributeValue) {
        return new Options(attributeValue.m().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue().bool() != null) {
                        return entry.getValue().bool();
                    } else if (entry.getValue().n() != null) {
                        return Float.parseFloat(entry.getValue().n());
                    } else {
                        return entry.getValue().s();
                    }
                })));
    }

    private Player mapToPlayer(AttributeValue attributeValue) {
        Map<String, AttributeValue> map = attributeValue.m();
        return Player.builder()
                .id(Player.Id.of(map.get("Id").s()))
                .type(map.containsKey("Type") ? Player.Type.valueOf(map.get("Type").s()) : Player.Type.USER)
                .userId(map.containsKey("UserId") ? User.Id.of(map.get("UserId").s()) : null)
                .status(Player.Status.valueOf(map.get("Status").s()))
                .color(map.containsKey("Color") ? PlayerColor.valueOf(map.get("Color").s()) : null)
                .score(map.containsKey("Score") ? Integer.parseInt(map.get("Score").n()) : null)
                .winner(map.containsKey("Winner") ? map.get("Winner").bool() : null)
                .created(Instant.parse(map.get("Created").s()))
                .updated(Instant.parse(map.get("Updated").s()))
                .turn(map.containsKey("Turn") ? map.get("Turn").bool() : false)
                .turnLimit(map.containsKey("TurnLimit") ? Instant.parse(map.get("TurnLimit").s()) : null)
                .forceEndTurns(map.containsKey("ForceEndTurns") ? Integer.parseInt(map.get("ForceEndTurns").n()) : 0)
                .build();
    }

    public void delete(Table.Id id) {
        client.queryPaginator(QueryRequest.builder()
                .tableName(config.tableName())
                .keyConditionExpression(PK + "=:PK")
                .expressionAttributeValues(Map.of(":PK", Item.s(TABLE_PREFIX + id.getId())))
                .projectionExpression(PK + "," + SK)
                .build())
                .items()
                .stream()
                .peek(key -> System.out.println("Delete Request: " + key))
                .map(key -> WriteRequest.builder()
                        .deleteRequest(DeleteRequest.builder()
                                .key(key)
                                .build())
                        .build())
                .collect(Chunked.chunked(MAX_BATCH_WRITE_SIZE))
                .forEach(chunk ->
                        client.batchWriteItem(BatchWriteItemRequest.builder()
                                .requestItems(Map.of(config.tableName(), chunk))
                                .build()));
    }

    /**
     * @return stream of ids, no specific order is guaranteed
     */
    public Stream<Table.Id> findAllIds(Game.Id gameId) {
        return IntStream.range(0, config.readGameIdShards())
                // Scatter
                .parallel()
                .mapToObj(shard -> client.queryPaginator(QueryRequest.builder()
                        .tableName(config.tableName())
                        .indexName(GSI4)
                        .scanIndexForward(false)
                        .keyConditionExpression(GSI4PK + "=:GSI4PK AND begins_with(" + GSI4SK + ",:GSI4SK)")
                        .expressionAttributeValues(Map.of(
                                ":GSI4PK", Item.s(GAME_PREFIX + gameId.getId() + "#" + shard),
                                ":GSI4SK", Item.s(TABLE_PREFIX)
                        ))
                        .build())
                        .items()
                        .stream())
                // Gather
                .flatMap(Function.identity())
                .map(item -> Table.Id.of(item.get(PK).s().replace(TABLE_PREFIX, "")));
    }

    @Value
    private static class GSISK {

        Table.Id tableId;
        Game.Id gameId;

        static String fromTable(Table table) {
            switch (table.getStatus()) {
                case ENDED:
                    return from(table.getId(), table.getStatus(), table.getEnded(), table.getGame().getId());
                case ABANDONED:
                    return from(table.getId(), table.getStatus(), table.getStarted() != null ? table.getStarted() : table.getCreated(), table.getGame().getId());
                case STARTED:
                    return from(table.getId(), table.getStatus(), table.getStarted(), table.getGame().getId());
                case NEW:
                    return from(table.getId(), table.getStatus(), table.getCreated(), table.getGame().getId());
                default:
                    throw new IllegalStateException(String.format("Table '%s' not in valid status for " + GSI1SK + ": %s", table.getId().getId(), table.getStatus()));
            }
        }

        public static String from(Table.Id tableId, Table.Status status, Instant timestamp, Game.Id gameId) {
            return partial(status, timestamp)
                    + "#" + tableId.getId()
                    + "#" + gameId.getId();
        }

        public static String partial(Table.Status status, Instant timestamp) {
            return TABLE_PREFIX + orderFromStatus(status)
                    + "#" + TIMESTAMP_SECS_FORMATTER.format(timestamp);
        }

        private static String orderFromStatus(Table.Status status) {
            // Ascending: ENDED -> ABANDONED -> STARTED -> NEW
            switch (status) {
                case ENDED:
                    return "10";
                case ABANDONED:
                    return "20";
                case STARTED:
                    return "30";
                case NEW:
                    return "40";
                default:
                    throw new IllegalStateException(String.format("Not valid status for " + GSI1SK + ": %s", status));
            }
        }

        public static GSISK parse(String sk) {
            if (!sk.startsWith(TABLE_PREFIX)) {
                throw new IllegalArgumentException("Sort key does not start with " + TABLE_PREFIX + ": " + sk);
            }
            var parts = sk.split("#", 5);
            if (parts.length != 5) {
                throw new IllegalArgumentException("Sort key not recognized: " + sk);
            }
            return new GSISK(Table.Id.of(parts[3]), Game.Id.of(parts[4]));
        }

    }

}
