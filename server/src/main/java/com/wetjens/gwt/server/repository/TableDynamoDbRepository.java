package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.api.Game;
import com.wetjens.gwt.api.Options;
import com.wetjens.gwt.api.PlayerColor;
import com.wetjens.gwt.api.State;
import com.wetjens.gwt.server.domain.*;
import lombok.NonNull;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TableDynamoDbRepository implements Tables {

    private static final String TABLE_NAME = "gwt-games";
    private static final String LOG_TABLE_NAME = "gwt-log";

    private static final String USER_ID_ID_INDEX = "UserId-Id-index";

    private final Games games;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String logTableName;

    @Inject
    public TableDynamoDbRepository(@NonNull Games games,
                                   @NonNull DynamoDbClient dynamoDbClient,
                                   @NonNull DynamoDbConfiguration config) {
        this.games = games;
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
        this.logTableName = LOG_TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Table findById(Table.Id id) {
        return findOptionallyById(id)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Stream<Table> findByUserId(User.Id userId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .expressionAttributeValues(Collections.singletonMap(":UserId", AttributeValue.builder()
                        .s("User-" + userId.getId())
                        .build()))
                .build());

        return response.items().stream()
                .flatMap(item -> findOptionallyById(Table.Id.of(item.get("Id").s())).stream());
    }

    @Override
    public int countActiveRealtimeByUserId(User.Id userId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .filterExpression("#Type = :Type AND #Status <> :NotStatus")
                .expressionAttributeNames(Map.of(
                        "#Type", "Type",
                        "#Status", "Status"))
                .expressionAttributeValues(Map.of(
                        ":UserId", AttributeValue.builder().s("User-" + userId.getId()).build(),
                        ":Type", AttributeValue.builder().s(Table.Type.REALTIME.name()).build(),
                        ":NotStatus", AttributeValue.builder().s(Table.Status.ABANDONED.name()).build()))
                .select(Select.COUNT)
                .build());

        return response.count();
    }

    @Override
    public void add(Table table) {
        var item = mapFromTable(table);

        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(
                        tableName, Stream.concat(
                                Stream.of(WriteRequest.builder().putRequest(
                                        PutRequest.builder()
                                                .item(item)
                                                .build())
                                        .build()),
                                table.getPlayers().stream()
                                        .filter(player -> player.getType() == Player.Type.USER)
                                        .map(player -> mapLookupItem(table, player))
                                        .map(lookupItem -> WriteRequest.builder()
                                                .putRequest(PutRequest.builder()
                                                        .item(lookupItem)
                                                        .build())
                                                .build()))
                                .collect(Collectors.toList())))
                .build());

        addLogEntries(table);
    }

    private void addLogEntries(Table table) {
        var log = table.getLog();

        var logEntries = log instanceof LazyLog
                ? ((LazyLog) log).pending()
                : log.stream();

        List<WriteRequest> writeRequests = logEntries
                .map(logEntry -> mapFromLogEntry(table.getId(), logEntry))
                .map(logItem -> WriteRequest.builder()
                        .putRequest(PutRequest.builder()
                                .item(logItem)
                                .build())
                        .build())
                .collect(Collectors.toList());

        if (!writeRequests.isEmpty()) {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(
                            logTableName, writeRequests))
                    .build());
        }
    }

    @Override
    public void update(Table table) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(table.getId()))
                .attributeUpdates(mapFromTableUpdate(table))
                .build());

        addLogEntries(table);

        updateLookupItems(table);
    }

    private void updateLookupItems(Table table) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("Id = :Id")
                .expressionAttributeValues(Collections.singletonMap(":Id", AttributeValue.builder()
                        .s(table.getId().getId())
                        .build()))
                .build());

        var lookupItemsBySortKey = response.items().stream()
                .filter(item -> !item.get("UserId").s().equals("Game-" + table.getId().getId())) // Filter out the main item
                .collect(Collectors.toMap(item -> item.get("UserId").s(), Function.identity()));

        var playersBySortKey = table.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .collect(Collectors.toMap(player -> "User-" + player.getUserId().getId(), Function.identity()));

        var lookupItemsToDelete = lookupItemsBySortKey.entrySet().stream()
                .filter(item -> !playersBySortKey.containsKey(item.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        var lookupItemsToAdd = playersBySortKey.entrySet().stream()
                .filter(entry -> !lookupItemsBySortKey.containsKey(entry.getKey()))
                .map(entry -> mapLookupItem(table, entry.getValue()))
                .collect(Collectors.toList());

        if (!lookupItemsToAdd.isEmpty() || !lookupItemsToDelete.isEmpty()) {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(tableName, Stream.concat(
                            lookupItemsToDelete.stream().map(item -> WriteRequest.builder()
                                    .deleteRequest(DeleteRequest.builder()
                                            .key(item.entrySet().stream()
                                                    .filter(entry -> entry.getKey().equals("Id") || entry.getKey().equals("UserId"))
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                                            .build())
                                    .build()),
                            lookupItemsToAdd.stream().map(item -> WriteRequest.builder()
                                    .putRequest(PutRequest.builder()
                                            .item(item)
                                            .build())
                                    .build()))
                            .collect(Collectors.toList())))
                    .build());
        }

        playersBySortKey.entrySet().stream()
                .filter(entry -> lookupItemsBySortKey.containsKey(entry.getKey()))
                .forEach(entry -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                        .tableName(tableName)
                        .key(keyLookup(table.getId(), entry.getValue().getUserId()))
                        .attributeUpdates(mapLookupItemUpdate(table, entry.getValue()))
                        .build()));
    }

    private Optional<Table> findOptionallyById(Table.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .consistentRead(true)
                .attributesToGet("Id",
                        "Type",
                        "Game",
                        "Status",
                        "Options",
                        "Created",
                        "Updated",
                        "Started",
                        "Ended",
                        "Expires",
                        "OwnerUserId",
                        "Players")
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(mapToTable(response.item()));
    }

    private Map<String, AttributeValue> mapFromTable(Table table) {
        var map = createAttributeValues(table);
        map.putAll(key(table.getId()));
        return map;
    }

    private Map<String, AttributeValue> mapLookupItem(Table table, Player player) {
        var map = new HashMap<>(keyLookup(table.getId(), player.getUserId()));
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());
        map.put("Expires", AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build());
        return map;
    }

    private Map<String, AttributeValueUpdate> mapLookupItemUpdate(Table table, Player player) {
        var map = new HashMap<String, AttributeValueUpdate>();
        map.put("Status", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(player.getStatus().name()).build()).build());
        map.put("Expires", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build()).build());
        return map;
    }

    private Map<String, AttributeValue> createAttributeValues(Table table) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Game", AttributeValue.builder().s(table.getGame().getId()).build());
        map.put("Type", AttributeValue.builder().s(table.getType().name()).build());
        map.put("Status", AttributeValue.builder().s(table.getStatus().name()).build());
        map.put("Options", AttributeValue.builder().m(table.getOptions().asMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue() instanceof Boolean) {
                        return AttributeValue.builder().bool((Boolean) entry.getValue()).build();
                    } else if (entry.getValue() instanceof Number) {
                        return AttributeValue.builder().n(entry.getValue().toString()).build();
                    } else {
                        return AttributeValue.builder().s(entry.getValue().toString()).build();
                    }
                })))
                .build());
        map.put("Created", AttributeValue.builder().n(Long.toString(table.getCreated().getEpochSecond())).build());
        map.put("Updated", AttributeValue.builder().n(Long.toString(table.getUpdated().getEpochSecond())).build());
        map.put("Started", table.getStarted() != null ? AttributeValue.builder().n(Long.toString(table.getStarted().getEpochSecond())).build() : null);
        map.put("Ended", table.getEnded() != null ? AttributeValue.builder().n(Long.toString(table.getEnded().getEpochSecond())).build() : null);
        map.put("Expires", AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build());
        map.put("OwnerUserId", AttributeValue.builder().s(table.getOwner().getId()).build());
        map.put("Players", AttributeValue.builder().l(table.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build());
        if (table.getState() != null) {
            map.put("State", mapFromState(table.getState().get()));
        }
        return map;
    }

    private Table mapToTable(Map<String, AttributeValue> item) {
        var id = Table.Id.of(item.get("Id").s());

        var Game = games.get(item.get("Game").s());

        return Table.builder()
                .id(id)
                .type(Table.Type.valueOf(item.get("Type").s()))
                .game(Game)
                .status(Table.Status.valueOf(item.get("Status").s()))
                .options(new Options(item.get("Options").m().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                            if (entry.getValue().bool() != null) {
                                return entry.getValue().bool();
                            } else if (entry.getValue().n() != null) {
                                return Float.parseFloat(entry.getValue().n());
                            } else {
                                return entry.getValue().s();
                            }
                        }))))
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
                .started(item.get("Started") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Started").n())) : null)
                .ended(item.get("Ended") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Ended").n())) : null)
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .owner(User.Id.of(item.get("OwnerUserId").s()))
                .players(item.get("Players").l().stream()
                        .map(this::mapToPlayer)
                        .collect(Collectors.toSet()))
                .state(Lazy.defer(() -> getState(Game, id)))
                .log(new LazyLog(since -> findLogEntries(id, since)))
                .build();
    }

    private State getState(Game game, Table.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .consistentRead(true)
                .attributesToGet("State", "Game")
                .build());

        var attributeValue = response.item().get("State");

        if (attributeValue != null) {
            return game.deserialize(attributeValue.b().asInputStream());
        }
        return null;
    }

    private Player mapToPlayer(AttributeValue attributeValue) {
        Map<String, AttributeValue> map = attributeValue.m();
        return Player.builder()
                .id(Player.Id.of(map.get("Id").s()))
                .type(map.containsKey("Type") ? Player.Type.valueOf(map.get("Type").s()) : Player.Type.USER)
                .userId(map.containsKey("UserId") ? User.Id.of(map.get("UserId").s()) : null)
                .status(Player.Status.valueOf(map.get("Status").s()))
                .color(map.containsKey("Color") ? PlayerColor.valueOf(map.get("Color").s()) : null)
                .score(map.containsKey("Score") ? mapToScore(map.get("Score")) : null)
                .winner(map.containsKey("Winner") ? map.get("Winner").bool() : null)
                .created(Instant.ofEpochSecond(Long.parseLong(map.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(map.get("Updated").n())))
                .mustRespondBefore(map.containsKey("MustRespondBefore") ? Instant.ofEpochSecond(Long.parseLong(map.get("MustRespondBefore").n())) : null)
                .build();
    }

    private Score mapToScore(AttributeValue attributeValue) {
        Map<String, AttributeValue> map = attributeValue.m();
        return new Score(map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Integer.valueOf(entry.getValue().n()))));
    }

    private AttributeValue mapFromState(State state) {
        try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
            state.serialize(byteArrayOutputStream);

            return AttributeValue.builder().b(SdkBytes.fromByteArray(byteArrayOutputStream.toByteArray())).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private AttributeValue mapFromPlayer(Player player) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Id", AttributeValue.builder().s(player.getId().getId()).build());
        map.put("Type", AttributeValue.builder().s(player.getType().name()).build());
        map.put("UserId", player.getUserId() != null ? AttributeValue.builder().s(player.getUserId().getId()).build() : null);
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());
        map.put("Color", player.getColor() != null ? AttributeValue.builder().s(player.getColor().name()).build() : null);
        map.put("Score", player.getScore() != null ? mapFromScore(player.getScore()) : null);
        map.put("Winner", player.getWinner() != null ? AttributeValue.builder().bool(player.getWinner()).build() : null);
        map.put("Created", AttributeValue.builder().n(Long.toString(player.getCreated().getEpochSecond())).build());
        map.put("Updated", AttributeValue.builder().n(Long.toString(player.getUpdated().getEpochSecond())).build());
        map.put("MustRespondBefore", player.getMustRespondBefore() != null ? AttributeValue.builder().n(Long.toString(player.getMustRespondBefore().getEpochSecond())).build() : null);
        return AttributeValue.builder().m(map).build();
    }

    private AttributeValue mapFromScore(Score score) {
        return AttributeValue.builder().m(score.getCategories().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> AttributeValue.builder()
                        .n(entry.getValue().toString())
                        .build()))).build();
    }

    private Map<String, AttributeValue> key(Table.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        key.put("UserId", AttributeValue.builder().s("Game-" + id.getId()).build());
        return key;
    }

    private Map<String, AttributeValue> keyLookup(Table.Id gameId, User.Id userId) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(gameId.getId()).build());
        key.put("UserId", AttributeValue.builder().s("User-" + userId.getId()).build());
        return key;
    }

    private Map<String, AttributeValueUpdate> mapFromTableUpdate(Table table) {
        var map = new HashMap<String, AttributeValueUpdate>();

        map.put("Status", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(table.getStatus().name()).build()).build());
        map.put("Updated", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(table.getUpdated().getEpochSecond())).build()).build());
        if (table.getStarted() != null) {
            map.put("Started", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(table.getStarted().getEpochSecond())).build()).build());
        }
        if (table.getEnded() != null) {
            map.put("Ended", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(table.getEnded().getEpochSecond())).build()).build());
        }
        map.put("Expires", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build()).build());
        map.put("OwnerUserId", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(table.getOwner().getId()).build()).build());
        map.put("Players", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().l(table.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build()).build());

        if (table.getState().isResolved()) {
            map.put("State", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(mapFromState(table.getState().get())).build());
        }

        return map;
    }

    private Map<String, AttributeValue> mapFromLogEntry(Table.Id gameId, LogEntry logEntry) {
        var item = new HashMap<String, AttributeValue>();

        item.put("GameId", AttributeValue.builder().s(gameId.getId()).build());
        item.put("Timestamp", AttributeValue.builder().n(Long.toString(logEntry.getTimestamp().toEpochMilli())).build());
        item.put("UserId", logEntry.getUserId() != null ? AttributeValue.builder().s(logEntry.getUserId().getId()).build() : null);
        item.put("PlayerId", logEntry.getPlayerId() != null ? AttributeValue.builder().s(logEntry.getPlayerId().getId()).build() : null);
        item.put("Expires", AttributeValue.builder().n(Long.toString(logEntry.getExpires().getEpochSecond())).build());
        item.put("Type", AttributeValue.builder().s(logEntry.getType().name()).build());
        item.put("Parameters", AttributeValue.builder().l(logEntry.getParameters().stream()
                .map(param -> AttributeValue.builder().s(param).build())
                .collect(Collectors.toList()))
                .build());

        return item;
    }

    private LogEntry mapToLogEntry(Map<String, AttributeValue> item) {
        return LogEntry.builder()
                .timestamp(Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n())))
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .playerId(item.containsKey("PlayerId") ? Player.Id.of(item.get("PlayerId").s()) : null)
                .userId(item.containsKey("UserId") ? User.Id.of(item.get("UserId").s()) : null)
                .type(LogEntry.Type.valueOf(item.get("Type").s()))
                .parameters(item.get("Parameters").l().stream()
                        .map(AttributeValue::s)
                        .collect(Collectors.toList()))
                .build();
    }

    private Stream<LogEntry> findLogEntries(Table.Id gameId, Instant since) {
        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":GameId", AttributeValue.builder().s(gameId.getId()).build());
        expressionAttributeValues.put(":Since", AttributeValue.builder().n(Long.toString(since.toEpochMilli())).build());

        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(logTableName)
                .keyConditionExpression("GameId = :GameId AND #Timestamp > :Since")
                .expressionAttributeNames(Collections.singletonMap("#Timestamp", "Timestamp"))
                .expressionAttributeValues(expressionAttributeValues)
                .scanIndexForward(false)
                .build())
                .items().stream()
                .map(this::mapToLogEntry);
    }
}
