package com.wetjens.gwt.server.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.GameLog;
import com.wetjens.gwt.server.domain.GameLogEntry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@ApplicationScoped
@Slf4j
public class GameLogDynamoDbRepository implements GameLog {

    private static final String TABLE_NAME = "gwt-game-log";
    private static final String GAME_ID_TIMESTAMP_INDEX = "GameId-Timestamp-index";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public GameLogDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Stream<GameLogEntry> findSince(Game.Id gameId, Instant since) {
        return null;
    }

    @Override
    public void addAll(Collection<GameLogEntry> entries) {
        entries.stream()
                .map(entry -> {
                    var item = new HashMap<String, AttributeValue>();
                    item.put("GameId", AttributeValue.builder().s(game.getId().getId()).build());
                    item.put("Timestamp", AttributeValue.builder().n(Long.toString(Instant.now().getEpochSecond())).build());
                    item.put("UserId", AttributeValue.builder().s(user.getId().getId()).build());
                    item.put("Expires", AttributeValue.builder().n(Long.toString(game.getExpires().getEpochSecond())).build());
                    return item;
                });

        // TODO

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }
}
