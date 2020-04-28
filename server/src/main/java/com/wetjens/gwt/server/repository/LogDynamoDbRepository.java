package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.LogEntries;
import com.wetjens.gwt.server.domain.LogEntry;
import com.wetjens.gwt.server.domain.User;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Slf4j
public class LogDynamoDbRepository implements LogEntries {

    private static final String TABLE_NAME = "gwt-game-log";
    private static final String GAME_ID_TIMESTAMP_INDEX = "GameId-Timestamp-index";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public LogDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Stream<LogEntry> findSince(Game.Id gameId, Instant since) {
        return dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(tableName)
                .indexName(GAME_ID_TIMESTAMP_INDEX)
                .filterExpression("Timestamp > :Since")
                .expressionAttributeValues(Collections.singletonMap(":Since", AttributeValue.builder().n(Long.toString(since.getEpochSecond())).build()))
                .build())
                .items().stream()
                .map(this::mapToLogEntry);
    }

    @Override
    public void addAll(Collection<LogEntry> entries) {
        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Collections.singletonMap(tableName, entries.stream()
                        .map(this::mapToItem)
                        .map(item -> PutRequest.builder()
                                .item(item)
                                .build())
                        .map(putRequest -> WriteRequest.builder()
                                .putRequest(putRequest)
                                .build())
                        .collect(Collectors.toList())))
                .build());
    }

    @Override
    public void add(LogEntry entry) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(mapToItem(entry))
                .build());
    }

    private Map<String, AttributeValue> mapToItem(LogEntry entry) {
        var item = new HashMap<String, AttributeValue>();

        item.put("GameId", AttributeValue.builder().s(entry.getGameId().getId()).build());
        item.put("Timestamp", AttributeValue.builder().n(Long.toString(Instant.now().getEpochSecond())).build());
        item.put("UserId", AttributeValue.builder().s(entry.getUserId().getId()).build());
        item.put("Expires", AttributeValue.builder().n(Long.toString(entry.getExpires().getEpochSecond())).build());
        item.put("Type", AttributeValue.builder().s(entry.getType().name()).build());
        item.put("Values", AttributeValue.builder().l(entry.getValues().stream()
                .map(value -> value instanceof Number
                        ? AttributeValue.builder().n(value.toString()).build()
                        : AttributeValue.builder().s(value.toString()).build())
                .collect(Collectors.toList()))
                .build());

        return item;
    }

    private LogEntry mapToLogEntry(Map<String, AttributeValue> item) {
        return LogEntry.builder()
                .gameId(Game.Id.of(item.get("GameId").s()))
                .timestamp(Instant.ofEpochSecond(Long.parseLong(item.get("Timestamp").n())))
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .userId(User.Id.of(item.get("UserId").s()))
                .type(LogEntry.Type.valueOf(item.get("Type").s()))
                .values(item.get("Values").l().stream()
                        .map(attributeValue -> attributeValue.n() != null ? Float.parseFloat(attributeValue.n()) : attributeValue.s())
                        .collect(Collectors.toList()))
                .build();
    }

}
