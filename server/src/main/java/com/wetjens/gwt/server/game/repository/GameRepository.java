package com.wetjens.gwt.server.game.repository;

import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.game.domain.Games;
import com.wetjens.gwt.server.game.domain.Player;
import com.wetjens.gwt.server.user.domain.User;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GameRepository implements Games {

    private static final String TABLE_NAME = "gwt-games";
    private static final String USER_ID_ID_INDEX = "UserId-Id-index";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public GameRepository(@NonNull DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + getTableNameSuffix();
    }

    private String getTableNameSuffix() {
        return "prod".equalsIgnoreCase(System.getenv("ENV")) ? "" : "-test";
    }

    @Override
    public Game findById(Game.Id id) {
        return getItem(key(id))
                .orElseThrow(() -> new NotFoundException("Game not found: " + id));
    }

    @Override
    public Stream<Game> findByUserId(User.Id userId) {
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .expressionAttributeValues(Collections.singletonMap(":UserId", AttributeValue.builder()
                        .s("User-" + userId.getId())
                        .build()))
                .build());

        return response.items().stream()
                .flatMap(item -> getItem(key(Game.Id.of(item.get("Id").s()))).stream());
    }

    @Override
    public void add(Game game) {
        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Collections.singletonMap(tableName, Stream.concat(
                        Stream.of(WriteRequest.builder().putRequest(
                                PutRequest.builder()
                                        .item(createItem(game))
                                        .build())
                                .build()),
                        createLookupItems(game)
                                .map(item -> WriteRequest.builder()
                                        .putRequest(PutRequest.builder()
                                                .item(item)
                                                .build())
                                        .build()))
                        .collect(Collectors.toSet())))
                .build());
    }

    public Map<String, AttributeValue> createItem(Game game) {
        Map<String, AttributeValue> map = createAttributeValues(game);
        map.putAll(key(game.getId()));
        return map;
    }

    private Optional<Game> getItem(Map<String, AttributeValue> key) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .consistentRead(true)
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }

        Map<String, AttributeValue> item = response.item();

        try {
            return Optional.of(Game.builder()
                    .id(Game.Id.of(item.get("Id").s()))
                    .owner(User.Id.of(item.get("OwnerUserId").s()))
                    .players(item.get("Players").l().stream()
                            .map(this::mapToPlayer)
                            .collect(Collectors.toSet()))
                    .state(com.wetjens.gwt.Game.deserialize(item.get("State").b().asInputStream()))
                    .build());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Player mapToPlayer(AttributeValue attributeValue) {
        return Player.builder()
                .userId(User.Id.of(attributeValue.m().get("UserId").s()))
                .status(Player.Status.valueOf(attributeValue.m().get("Status").s()))
                .build();
    }

    private Stream<Map<String, AttributeValue>> createLookupItems(Game game) {
        return game.getPlayers().stream()
                .map(player -> createLookupItem(game, player));
    }

    private Map<String, AttributeValue> createLookupItem(Game game, Player player) {
        var map = new HashMap<String, AttributeValue>();

        map.put("Id", AttributeValue.builder().s("Game-" + game.getId().getId()).build());
        map.put("UserId", AttributeValue.builder().s("User-" + player.getUserId().getId()).build());

        return map;
    }

    private Map<String, AttributeValue> createAttributeValues(Game game) {
        var map = new HashMap<String, AttributeValue>();

        map.put("Status", AttributeValue.builder().s(game.getStatus().name()).build());
        map.put("OwnerUserId", AttributeValue.builder().s(game.getOwner().getId()).build());
        map.put("Players", AttributeValue.builder().l(game.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build());

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            game.getState().serialize(byteArrayOutputStream);

            map.put("State", AttributeValue.builder().b(SdkBytes.fromByteArray(byteArrayOutputStream.toByteArray())).build());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return map;
    }

    private AttributeValue mapFromPlayer(Player player) {
        var map = new HashMap<String, AttributeValue>();

        map.put("UserId", AttributeValue.builder().s(player.getUserId().getId()).build());
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());

        return AttributeValue.builder().m(map).build();
    }

    private Map<String, AttributeValue> key(Game.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s("Game-" + id.getId()).build());
        key.put("UserId", AttributeValue.builder().s("User-" + id.getId()).build());
        return key;
    }

    @Override
    public void update(Game game) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .key(key(game.getId()))
                .attributeUpdates(createAttributeUpdateValues(game))
                .build());
    }

    private Map<String, AttributeValueUpdate> createAttributeUpdateValues(Game game) {
        Map<String, AttributeValue> attributeValues = createAttributeValues(game);

        return attributeValues.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> AttributeValueUpdate.builder()
                        .action(AttributeAction.PUT)
                        .value(entry.getValue()).build()));
    }

}
