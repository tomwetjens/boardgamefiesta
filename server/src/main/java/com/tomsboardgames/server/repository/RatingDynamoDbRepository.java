package com.tomsboardgames.server.repository;

import com.tomsboardgames.server.domain.Table;
import com.tomsboardgames.server.domain.User;
import com.tomsboardgames.server.domain.rating.Rating;
import com.tomsboardgames.server.domain.rating.Ratings;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class RatingDynamoDbRepository implements Ratings {

    private static final String TABLE_NAME = "gwt-ratings";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public RatingDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Stream<Rating> findHistoric(User.Id userId, String gameId, Instant from, Instant to) {
        var response = dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("UserIdGameId = :UserIdGameId AND Timestamp BETWEEN :From AND :To")
                .expressionAttributeValues(Map.of(
                        ":UserIdGameId", AttributeValue.builder().s(partitionKey(userId, gameId)).build(),
                        ":From", AttributeValue.builder().n(Long.toString(from.toEpochMilli())).build(),
                        ":To", AttributeValue.builder().n(Long.toString(to.toEpochMilli())).build()))
                .build());

        return response.items().stream()
                .map(this::mapToRating);
    }

    @Override
    public Optional<Rating> findByTable(User.Id userId, Table table) {
        // Restrict the time period to limit scanning
        var from = table.getEnded();
        // Reasonable assumption that rating is stored within after the game has ended
        var to = table.getEnded().plus(1, ChronoUnit.HOURS);

        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("UserIdGameId = :UserIdGameId AND Timestamp BETWEEN :From AND :To")
                .filterExpression("TableId = :TableId")
                .expressionAttributeValues(Map.of(
                        ":UserIdGameId", AttributeValue.builder().s(partitionKey(userId, table.getGame().getId())).build(),
                        ":From", AttributeValue.builder().n(Long.toString(from.toEpochMilli())).build(),
                        ":To", AttributeValue.builder().n(Long.toString(to.toEpochMilli())).build()))
                .limit(1)
                .build());

        if (!response.hasItems() || response.count() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapToRating(response.items().get(0)));
    }

    @Override
    public Rating findLatest(User.Id userId, String gameId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("UserIdGameId = :UserIdGameId")
                .expressionAttributeValues(Collections.singletonMap(":UserIdGameId",
                        AttributeValue.builder().s(partitionKey(userId, gameId)).build()))
                .scanIndexForward(false) // Descending to find latest
                .limit(1)
                .build());

        if (!response.hasItems() || response.count() == 0) {
            return Rating.initial(userId, gameId);
        }
        return mapToRating(response.items().get(0));
    }

    private String partitionKey(User.Id userId, String gameId) {
        return userId.getId() + " " + gameId;
    }

    private Rating mapToRating(Map<String, AttributeValue> attributeValues) {
        var parts = attributeValues.get("UserIdGameId").s().split(" ");
        var gameId = parts[0];
        var userId = User.Id.of(parts[1]);

        return Rating.builder()
                .userId(userId)
                .timestamp(Instant.ofEpochMilli(Long.parseLong(attributeValues.get("Timestamp").n())))
                .expires(Instant.ofEpochMilli(Long.parseLong(attributeValues.get("Expires").n())))
                .gameId(gameId)
                .tableId(attributeValues.containsKey("TableId") ? Table.Id.of(attributeValues.get("TableId").s()) : null)
                .rating(Float.parseFloat(attributeValues.get("Rating").n()))
                .deltas(mapToDeltas(attributeValues.get("Deltas").m()))
                .build();
    }

    private Map<User.Id, Float> mapToDeltas(Map<String, AttributeValue> attributeValues) {
        return attributeValues.entrySet().stream()
                .collect(Collectors.toMap(entry -> User.Id.of(entry.getKey()), entry -> Float.parseFloat(entry.getValue().n())));
    }

    @Override
    public void addAll(Collection<Rating> ratings) {
        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(tableName, ratings.stream()
                        .map(rating -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(mapFromRating(rating))
                                        .build())
                                .build())
                        .collect(Collectors.toList())))
                .build());
    }

    private Map<String, AttributeValue> mapFromRating(Rating rating) {
        var map = new HashMap<String, AttributeValue>();
        map.put("UserIdGameId", AttributeValue.builder().s(partitionKey(rating.getUserId(), rating.getGameId())).build());
        map.put("Timestamp", AttributeValue.builder().n(Long.toString(rating.getTimestamp().toEpochMilli())).build());
        map.put("TableId", rating.getTableId().map(tableId -> AttributeValue.builder().s(tableId.getId()).build()).orElse(null));
        map.put("Rating", AttributeValue.builder().n(Float.toString(rating.getRating())).build());
        map.put("Deltas", mapFromDeltas(rating.getDeltas()));
        return map;
    }

    private AttributeValue mapFromDeltas(Map<User.Id, Float> deltas) {
        return AttributeValue.builder()
                .m(deltas.entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().getId(),
                        entry -> AttributeValue.builder().n(Float.toString(entry.getValue())).build())))
                .build();
    }
}
