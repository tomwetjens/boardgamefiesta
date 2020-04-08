package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.Player;
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

    private static final String TABLE_NAME = "gwt-games";
    private static final String USER_ID_ID_INDEX = "UserId-Id-index";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public GameDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Game findById(Game.Id id) {
        return findOptionallyById(id)
                .orElseThrow(() -> new NotFoundException("Game not found: " + id));
    }

    @Override
    public Stream<Game> findByUserId(User.Id userId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
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
    public void add(Game game) {
        var item = createItem(game);

        var lookupItems = game.getPlayers().stream()
                .map(player -> createItemLookup(game, player));

        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Collections.singletonMap(tableName, Stream.concat(
                        Stream.of(WriteRequest.builder().putRequest(
                                PutRequest.builder()
                                        .item(item)
                                        .build())
                                .build()),
                        lookupItems.map(lookupItem -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(lookupItem)
                                        .build())
                                .build()))
                        .collect(Collectors.toSet())))
                .build());
    }

    @Override
    public void update(Game game) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(game.getId()))
                .attributeUpdates(overwriteAllValues(createAttributeValues(game)))
                .build());

        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("Id = :Id")
                .expressionAttributeValues(Collections.singletonMap(":Id", AttributeValue.builder()
                        .s(game.getId().getId())
                        .build()))
                .build());

        var lookupItemsBySortKey = response.items().stream()
                .filter(item -> !item.get("UserId").s().equals("Game-" + game.getId().getId())) // Filter out the main item
                .collect(Collectors.toMap(item -> item.get("UserId").s(), Function.identity()));

        var playersBySortKey = game.getPlayers().stream()
                .collect(Collectors.toMap(player -> "User-" + player.getUserId().getId(), Function.identity()));

        var lookupItemsToDelete = lookupItemsBySortKey.entrySet().stream()
                .filter(item -> !playersBySortKey.containsKey(item.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        var lookupItemsToAdd = playersBySortKey.entrySet().stream()
                .filter(entry -> !lookupItemsBySortKey.containsKey(entry.getKey()))
                .map(entry -> createItemLookup(game, entry.getValue()))
                .collect(Collectors.toList());

        if (!lookupItemsToAdd.isEmpty() || !lookupItemsToDelete.isEmpty()) {
            var addAndDeleteLookupItems = BatchWriteItemRequest.builder()
                    .requestItems(Collections.singletonMap(tableName, Stream.concat(
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
                    .build();

            dynamoDbClient.batchWriteItem(addAndDeleteLookupItems);
        }

        playersBySortKey.entrySet().stream()
                .filter(entry -> lookupItemsBySortKey.containsKey(entry.getKey()))
                .forEach(entry -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                        .tableName(tableName)
                        .key(keyLookup(game.getId(), entry.getValue().getUserId()))
                        .attributeUpdates(overwriteAllValues(createAttributeValuesLookup(game, entry.getValue())))
                        .build()));
    }

    private Optional<Game> findOptionallyById(Game.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .consistentRead(true)
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(mapToGame(response.item()));
    }

    private Map<String, AttributeValue> createItem(Game game) {
        var map = createAttributeValues(game);
        map.putAll(key(game.getId()));
        return map;
    }

    private Map<String, AttributeValue> createItemLookup(Game game, Player player) {
        var map = createAttributeValuesLookup(game, player);
        map.putAll(keyLookup(game.getId(), player.getUserId()));
        return map;
    }

    private Map<String, AttributeValue> createAttributeValuesLookup(Game game, Player player) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());
        map.put("Expires", AttributeValue.builder().n(Long.toString(game.getExpires().getEpochSecond())).build());
        return map;
    }

    private Map<String, AttributeValue> createAttributeValues(Game game) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Status", AttributeValue.builder().s(game.getStatus().name()).build());
        map.put("Created", AttributeValue.builder().n(Long.toString(game.getCreated().getEpochSecond())).build());
        map.put("Updated", AttributeValue.builder().n(Long.toString(game.getUpdated().getEpochSecond())).build());
        map.put("Started", game.getStarted() != null ? AttributeValue.builder().n(Long.toString(game.getStarted().getEpochSecond())).build() : null);
        map.put("Ended", game.getEnded() != null ? AttributeValue.builder().n(Long.toString(game.getEnded().getEpochSecond())).build() : null);
        map.put("Expires", AttributeValue.builder().n(Long.toString(game.getExpires().getEpochSecond())).build());
        map.put("OwnerUserId", AttributeValue.builder().s(game.getOwner().getId()).build());
        map.put("Players", AttributeValue.builder().l(game.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build());
        map.put("State", mapFromState(game.getState()));
        return map;
    }

    private Game mapToGame(Map<String, AttributeValue> item) {
        return Game.builder()
                .id(Game.Id.of(item.get("Id").s()))
                .status(Game.Status.valueOf(item.get("Status").s()))
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
                .started(item.get("Started") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Started").n())) : null)
                .ended(item.get("Ended") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Ended").n())) : null)
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .owner(User.Id.of(item.get("OwnerUserId").s()))
                .players(item.get("Players").l().stream()
                        .map(this::mapToPlayer)
                        .collect(Collectors.toSet()))
                .state(mapToState(item.get("State")))
                .build();
    }

    private Player mapToPlayer(AttributeValue attributeValue) {
        return Player.builder()
                .userId(User.Id.of(attributeValue.m().get("UserId").s()))
                .status(Player.Status.valueOf(attributeValue.m().get("Status").s()))
                .created(Instant.ofEpochSecond(Long.parseLong(attributeValue.m().get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(attributeValue.m().get("Updated").n())))
                .build();
    }

    private AttributeValue mapFromState(com.wetjens.gwt.Game state) {
        if (state == null) {
            return null;
        }

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

        try {
            return com.wetjens.gwt.Game.deserialize(attributeValue.b().asInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private AttributeValue mapFromPlayer(Player player) {
        var map = new HashMap<String, AttributeValue>();
        map.put("UserId", AttributeValue.builder().s(player.getUserId().getId()).build());
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());
        map.put("Created", AttributeValue.builder().n(Long.toString(player.getCreated().getEpochSecond())).build());
        map.put("Updated", AttributeValue.builder().n(Long.toString(player.getUpdated().getEpochSecond())).build());
        return AttributeValue.builder().m(map).build();
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

    private static Map<String, AttributeValueUpdate> overwriteAllValues(Map<String, AttributeValue> attributeValues) {
        return attributeValues.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue() != null) {
                        return AttributeValueUpdate.builder()
                                .action(AttributeAction.PUT)
                                .value(entry.getValue()).build();
                    } else {
                        return AttributeValueUpdate.builder().action(AttributeAction.DELETE).build();
                    }
                }));
    }
}
