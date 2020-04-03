package com.wetjens.gwt.server.game.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.game.domain.Games;
import com.wetjens.gwt.server.game.domain.Player;
import com.wetjens.gwt.server.user.domain.User;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@ApplicationScoped
public class GameRepository implements Games {

    private static final String PK = "Id";
    private static final String SK = "SubId";
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public GameRepository(@NonNull DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = "prod".equals(System.getenv("ENV")) ? "gwt" : "gwt-test";
    }

    @Override
    public Game findById(Game.Id id) {
        AttributeValue pk = AttributeValue.builder().s("Game-" + id.getId()).build();
        return getItem(pk).orElseThrow(() -> new NotFoundException("Game not found: " + id));
    }

    private Optional<Game> getItem(AttributeValue pk) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PK, pk);

        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("Id = :Id")
                .expressionAttributeValues(Collections.singletonMap(":Id", pk))
                .build());

        return response.items().stream()
                .filter(item -> item.get("SubId").equals(pk))
                .findAny()
                .map(item -> Game.builder()
                        .id(Game.Id.of(item.get("Id").s()))
                        .status(Game.Status.valueOf(item.get("Status").s()))
                        .owner(User.Id.of(item.get("OwnerUserId").s()))
                        .players(response.items().stream()
                                .filter(item2 -> !item2.get("SubId").equals(pk))
                                .map(item2 -> Player.builder()
                                        .userId(User.Id.of(item2.get("UserId").s()))
                                        .status(Player.Status.valueOf(item2.get("Status").s()))
                                        .build())
                                .collect(Collectors.toSet()))
                        .build());
    }

    @Override
    public Stream<Game> findByUserId(User.Id userId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(SK, AttributeValue.builder()
                .s("User-" + userId)
                .build());

        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName("IdsBySubId-index")
                .keyConditionExpression("SubId = :SubId")
                .expressionAttributeValues(Collections.singletonMap(":SubId", AttributeValue.builder()
                        .s(userId.getId())
                        .build()))
                .build());

        return response.items().stream()
                .flatMap(item -> getItem(item.get(PK), item.get(PK)).stream());
    }

    @Override
    public void add(Game game) {
    }

    @Override
    public void update(Game game) {
    }
}
