package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.PlayerColor;
import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.Lazy;
import com.wetjens.gwt.server.domain.LogEntry;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.Score;
import com.wetjens.gwt.server.domain.User;
import lombok.NonNull;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GameDynamoDbRepository implements Games {

    private static final String GAME_TABLE_NAME = "gwt-games";
    private static final String LOG_TABLE_NAME = "gwt-log";

    private static final String USER_ID_ID_INDEX = "UserId-Id-index";

    private final DynamoDbClient dynamoDbClient;
    private final String gameTableName;
    private final String logTableName;

    @Inject
    public GameDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient,
                                  @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.gameTableName = GAME_TABLE_NAME + config.getTableSuffix().orElse("");
        this.logTableName = LOG_TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Game findById(Game.Id id) {
        return findOptionallyById(id)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Stream<Game> findByUserId(User.Id userId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(gameTableName)
                .indexName(USER_ID_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .expressionAttributeValues(Collections.singletonMap(":UserId", AttributeValue.builder()
                        .s("User-" + userId.getId())
                        .build()))
                .build());

        return response.items().stream()
                .flatMap(item -> findOptionallyById(Game.Id.of(item.get("Id").s())).stream());
    }

    @Override
    public int countByUserId(User.Id userId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(gameTableName)
                .indexName(USER_ID_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .expressionAttributeValues(Collections.singletonMap(":UserId", AttributeValue.builder()
                        .s("User-" + userId.getId())
                        .build()))
                .select(Select.COUNT)
                .build());

        return response.count();
    }

    @Override
    public void add(Game game) {
        var item = mapFromGame(game);

        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(
                        gameTableName, Stream.concat(
                                Stream.of(WriteRequest.builder().putRequest(
                                        PutRequest.builder()
                                                .item(item)
                                                .build())
                                        .build()),
                                game.getPlayers().stream()
                                        .filter(player -> player.getType() == Player.Type.USER)
                                        .map(player -> mapLookupItem(game, player))
                                        .map(lookupItem -> WriteRequest.builder()
                                                .putRequest(PutRequest.builder()
                                                        .item(lookupItem)
                                                        .build())
                                                .build()))
                                .collect(Collectors.toList())))
                .build());

        addLogEntries(game);
    }

    private void addLogEntries(Game game) {
        var log = game.getLog();

        var logEntries = log instanceof LazyLog
                ? ((LazyLog) log).pending()
                : log.stream();

        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(
                        logTableName, logEntries
                                .map(logEntry -> mapFromLogEntry(game.getId(), logEntry))
                                .map(logItem -> WriteRequest.builder()
                                        .putRequest(PutRequest.builder()
                                                .item(logItem)
                                                .build())
                                        .build())
                                .collect(Collectors.toList())))
                .build());
    }

    @Override
    public void update(Game game) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(gameTableName)
                .key(key(game.getId()))
                .attributeUpdates(mapFromGameUpdate(game))
                .build());

        addLogEntries(game);

        updateLookupItems(game);
    }

    private void updateLookupItems(Game game) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(gameTableName)
                .keyConditionExpression("Id = :Id")
                .expressionAttributeValues(Collections.singletonMap(":Id", AttributeValue.builder()
                        .s(game.getId().getId())
                        .build()))
                .build());

        var lookupItemsBySortKey = response.items().stream()
                .filter(item -> !item.get("UserId").s().equals("Game-" + game.getId().getId())) // Filter out the main item
                .collect(Collectors.toMap(item -> item.get("UserId").s(), Function.identity()));

        var playersBySortKey = game.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .collect(Collectors.toMap(player -> "User-" + player.getUserId().getId(), Function.identity()));

        var lookupItemsToDelete = lookupItemsBySortKey.entrySet().stream()
                .filter(item -> !playersBySortKey.containsKey(item.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        var lookupItemsToAdd = playersBySortKey.entrySet().stream()
                .filter(entry -> !lookupItemsBySortKey.containsKey(entry.getKey()))
                .map(entry -> mapLookupItem(game, entry.getValue()))
                .collect(Collectors.toList());

        if (!lookupItemsToAdd.isEmpty() || !lookupItemsToDelete.isEmpty()) {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(gameTableName, Stream.concat(
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
                        .tableName(gameTableName)
                        .key(keyLookup(game.getId(), entry.getValue().getUserId()))
                        .attributeUpdates(mapLookupItemUpdate(game, entry.getValue()))
                        .build()));
    }

    private Optional<Game> findOptionallyById(Game.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(gameTableName)
                .key(key(id))
                .consistentRead(true)
                .attributesToGet("Id", "Status", "Beginner", "Created", "Updated", "Started", "Ended", "Expires", "OwnerUserId", "Players")
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(mapToGame(response.item()));
    }

    private Map<String, AttributeValue> mapFromGame(Game game) {
        var map = createAttributeValues(game);
        map.putAll(key(game.getId()));
        return map;
    }

    private Map<String, AttributeValue> mapLookupItem(Game game, Player player) {
        var map = new HashMap<>(keyLookup(game.getId(), player.getUserId()));
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());
        map.put("Expires", AttributeValue.builder().n(Long.toString(game.getExpires().getEpochSecond())).build());
        return map;
    }

    private Map<String, AttributeValueUpdate> mapLookupItemUpdate(Game game, Player player) {
        var map = new HashMap<String, AttributeValueUpdate>();
        map.put("Status", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(player.getStatus().name()).build()).build());
        map.put("Expires", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(game.getExpires().getEpochSecond())).build()).build());
        return map;
    }

    private Map<String, AttributeValue> createAttributeValues(Game game) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Status", AttributeValue.builder().s(game.getStatus().name()).build());
        map.put("Beginner", AttributeValue.builder().bool(game.isBeginner()).build());
        map.put("Created", AttributeValue.builder().n(Long.toString(game.getCreated().getEpochSecond())).build());
        map.put("Updated", AttributeValue.builder().n(Long.toString(game.getUpdated().getEpochSecond())).build());
        map.put("Started", game.getStarted() != null ? AttributeValue.builder().n(Long.toString(game.getStarted().getEpochSecond())).build() : null);
        map.put("Ended", game.getEnded() != null ? AttributeValue.builder().n(Long.toString(game.getEnded().getEpochSecond())).build() : null);
        map.put("Expires", AttributeValue.builder().n(Long.toString(game.getExpires().getEpochSecond())).build());
        map.put("OwnerUserId", AttributeValue.builder().s(game.getOwner().getId()).build());
        map.put("Players", AttributeValue.builder().l(game.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build());
        if (game.getState() != null) {
            map.put("State", mapFromState(game.getState().get()));
        }
        return map;
    }

    private Game mapToGame(Map<String, AttributeValue> item) {
        var id = Game.Id.of(item.get("Id").s());

        return Game.builder()
                .id(id)
                .status(Game.Status.valueOf(item.get("Status").s()))
                .beginner(item.get("Beginner").bool())
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
                .started(item.get("Started") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Started").n())) : null)
                .ended(item.get("Ended") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Ended").n())) : null)
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .owner(User.Id.of(item.get("OwnerUserId").s()))
                .players(item.get("Players").l().stream()
                        .map(this::mapToPlayer)
                        .collect(Collectors.toSet()))
                .state(Lazy.defer(() -> getState(id)))
                .log(new LazyLog(since -> findLogEntries(id, since)))
                .build();
    }

    private com.wetjens.gwt.Game getState(Game.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(gameTableName)
                .key(key(id))
                .consistentRead(true)
                .attributesToGet("State")
                .build());

        return mapToState(response.item().get("State"));
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
                .build();
    }

    private Score mapToScore(AttributeValue attributeValue) {
        Map<String, AttributeValue> map = attributeValue.m();
        return new Score(map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Integer.valueOf(entry.getValue().n()))));
    }

    private AttributeValue mapFromState(com.wetjens.gwt.Game state) {
        try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
            state.serialize(byteArrayOutputStream);

            return AttributeValue.builder().b(SdkBytes.fromByteArray(byteArrayOutputStream.toByteArray())).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private com.wetjens.gwt.Game mapToState(AttributeValue attributeValue) {
        if (attributeValue == null) {
            return null;
        }

        return com.wetjens.gwt.Game.deserialize(attributeValue.b().asInputStream());
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
        return AttributeValue.builder().m(map).build();
    }

    private AttributeValue mapFromScore(Score score) {
        return AttributeValue.builder().m(score.getCategories().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> AttributeValue.builder()
                        .n(entry.getValue().toString())
                        .build()))).build();
    }

    private Map<String, AttributeValue> key(Game.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        key.put("UserId", AttributeValue.builder().s("Game-" + id.getId()).build());
        return key;
    }

    private Map<String, AttributeValue> keyLookup(Game.Id gameId, User.Id userId) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(gameId.getId()).build());
        key.put("UserId", AttributeValue.builder().s("User-" + userId.getId()).build());
        return key;
    }

    private Map<String, AttributeValueUpdate> mapFromGameUpdate(Game game) {
        var map = new HashMap<String, AttributeValueUpdate>();

        map.put("Status", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(game.getStatus().name()).build()).build());
        map.put("Updated", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(game.getUpdated().getEpochSecond())).build()).build());
        if (game.getStarted() != null) {
            map.put("Started", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(game.getStarted().getEpochSecond())).build()).build());
        }
        if (game.getEnded() != null) {
            map.put("Ended", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(game.getEnded().getEpochSecond())).build()).build());
        }
        map.put("Expires", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(game.getExpires().getEpochSecond())).build()).build());
        map.put("OwnerUserId", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(game.getOwner().getId()).build()).build());
        map.put("Players", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().l(game.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build()).build());

        if (game.getState().isResolved()) {
            map.put("State", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(mapFromState(game.getState().get())).build());
        }

        return map;
    }

    private Map<String, AttributeValue> mapFromLogEntry(Game.Id gameId, LogEntry logEntry) {
        var item = new HashMap<String, AttributeValue>();

        item.put("GameId", AttributeValue.builder().s(gameId.getId()).build());
        item.put("Timestamp", AttributeValue.builder().n(Long.toString(logEntry.getTimestamp().toEpochMilli())).build());
        item.put("PlayerId", AttributeValue.builder().s(logEntry.getPlayerId().getId()).build());
        item.put("Expires", AttributeValue.builder().n(Long.toString(logEntry.getExpires().getEpochSecond())).build());
        item.put("Type", AttributeValue.builder().s(logEntry.getType()).build());
        item.put("Values", AttributeValue.builder().l(logEntry.getValues().stream()
                .map(value -> value instanceof Number
                        ? AttributeValue.builder().n(value.toString()).build()
                        : AttributeValue.builder().s(value.toString()).build())
                .collect(Collectors.toList()))
                .build());

        return item;
    }

    private LogEntry mapToLogEntry(Map<String, AttributeValue> item) {
        return LogEntry.builder()
                .timestamp(Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n())))
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .playerId(Player.Id.of(item.get("PlayerId").s()))
                .type(item.get("Type").s())
                .values(item.get("Values").l().stream()
                        .map(attributeValue -> attributeValue.n() != null ? Float.parseFloat(attributeValue.n()) : attributeValue.s())
                        .collect(Collectors.toList()))
                .build();
    }

    private Stream<LogEntry> findLogEntries(Game.Id gameId, Instant since) {
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
