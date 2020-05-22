package com.tomsboardgames.server.repository;

import com.tomsboardgames.api.Game;
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
    private static final String RANKING_TABLE_NAME = "gwt-ranking";
    private static final String GAME_ID_RANK_ORDER_INDEX = "GameId-RankOrder-index";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String rankingTableName;

    @Inject
    public RatingDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
        this.rankingTableName = RANKING_TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Stream<Rating> findHistoric(User.Id userId, Game.Id gameId, Instant from, Instant to) {
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
    public Rating findLatest(User.Id userId, Game.Id gameId) {
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

    @Override
    public void addAll(Collection<Rating> ratings) {
        if (ratings.isEmpty()) {
            return;
        }

        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(
                        tableName, ratings.stream()
                                .map(rating -> WriteRequest.builder()
                                        .putRequest(PutRequest.builder()
                                                .item(mapFromRating(rating))
                                                .build())
                                        .build())
                                .collect(Collectors.toList()),
                        rankingTableName, ratings.stream()
                                .map(rating -> WriteRequest.builder()
                                        .putRequest(PutRequest.builder()
                                                .item(mapRankingFromRating(rating))
                                                .build())
                                        .build())
                                .collect(Collectors.toList())
                ))
                .build());
    }

    @Override
    public Stream<User.Id> findRanking(Game.Id gameId) {
        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(rankingTableName)
                .indexName("GameId-RankOrder-index")
                .keyConditionExpression("GameId = :GameId")
                .expressionAttributeValues(Map.of("GameId", AttributeValue.builder().s(gameId.getId()).build()))
                .scanIndexForward(false) // Descending, best player first
                .attributesToGet("UserId")
                .build())
                .items()
                .stream()
                .map(item -> User.Id.of(item.get("UserId").s()));
    }

    @Override
    public Optional<Integer> findRank(User.Id userId, Game.Id gameId) {
        return findRankOrder(userId, gameId)
                .map(rankOrder -> dynamoDbClient.query(QueryRequest.builder()
                        .tableName(rankingTableName)
                        .indexName(GAME_ID_RANK_ORDER_INDEX)
                        .keyConditionExpression("GameId = :GameId AND RankOrder > :RankOrder")
                        .expressionAttributeValues(Map.of("RankOrder", AttributeValue.builder().s(rankOrder).build()))
                        .select(Select.COUNT)
                        .build())
                        .count() + 1);
    }

    private Optional<String> findRankOrder(User.Id userId, Game.Id gameId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(rankingTableName)
                .keyConditionExpression("UserId = :UserId AND GameId = :GameId")
                .expressionAttributeValues(Map.of(
                        ":UserId", AttributeValue.builder().s(userId.getId()).build(),
                        ":GameId", AttributeValue.builder().s(gameId.getId()).build()
                ))
                .attributesToGet("RankOrder")
                .limit(1)
                .build());

        if (!response.hasItems()) {
            return Optional.empty();
        }
        return response.items().stream()
                .map(item -> item.get("RankOrder").s())
                .findFirst();
    }

    private String partitionKey(User.Id userId, Game.Id gameId) {
        return userId.getId() + " " + gameId.getId();
    }

    private Rating mapToRating(Map<String, AttributeValue> attributeValues) {
        return Rating.builder()
                .userId(User.Id.of(attributeValues.get("UserId").s()))
                .gameId(Game.Id.of(attributeValues.get("GameId").s()))
                .timestamp(Instant.ofEpochMilli(Long.parseLong(attributeValues.get("Timestamp").n())))
                .tableId(attributeValues.containsKey("TableId") ? Table.Id.of(attributeValues.get("TableId").s()) : null)
                .rating(Float.parseFloat(attributeValues.get("Rating").n()))
                .deltas(attributeValues.containsKey("Deltas") ? mapToDeltas(attributeValues.get("Deltas").m()) : Collections.emptyMap())
                .build();
    }

    private Map<User.Id, Float> mapToDeltas(Map<String, AttributeValue> attributeValues) {
        return attributeValues.entrySet().stream()
                .collect(Collectors.toMap(entry -> User.Id.of(entry.getKey()), entry -> Float.parseFloat(entry.getValue().n())));
    }

    private Map<String, AttributeValue> mapFromRating(Rating rating) {
        var map = new HashMap<String, AttributeValue>();
        map.put("UserIdGameId", AttributeValue.builder().s(partitionKey(rating.getUserId(), rating.getGameId())).build());
        map.put("UserId", AttributeValue.builder().s(rating.getUserId().getId()).build());
        map.put("GameId", AttributeValue.builder().s(rating.getGameId().getId()).build());
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

    private Map<String, AttributeValue> mapRankingFromRating(Rating rating) {
        var map = new HashMap<String, AttributeValue>();
        map.put("UserId", AttributeValue.builder().s(rating.getUserId().getId()).build());
        map.put("GameId", AttributeValue.builder().s(rating.getGameId().getId()).build());
        map.put("RankOrder", AttributeValue.builder().s(rankOrder(rating)).build());
        return map;
    }

    private String rankOrder(Rating rating) {
        // 1. By rating, with leading zeros (because String sorting)
        // 2. Then by timestamp, with leading zeros (because String sorting), in case 2 users have the same rating
        // 3. Then by user id, to make it guaranteed unique, in case 2 users have the same rating at the same time
        return String.format(Locale.ENGLISH, "%010.2f %019d %s",
                rating.getRating(),
                rating.getTimestamp().toEpochMilli(),
                rating.getUserId().getId());
    }
}
