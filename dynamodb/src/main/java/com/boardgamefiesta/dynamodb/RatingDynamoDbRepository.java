package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.rating.Ranking;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class RatingDynamoDbRepository implements Ratings {

    private static final String TABLE_NAME = "gwt-ratings";
    private static final String RANKING_TABLE_NAME = "gwt-ranking";
    private static final String GAME_ID_RANK_ORDER_INDEX = "GameId-RankOrder-index";
    private static final String TABLE_ID_USER_ID_INDEX = "TableId-UserId-index";

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
                .keyConditionExpression("UserIdGameId = :UserIdGameId AND #Timestamp BETWEEN :From AND :To")
                .expressionAttributeNames(Map.of("#Timestamp", "Timestamp"))
                .expressionAttributeValues(Map.of(
                        ":UserIdGameId", AttributeValue.builder().s(partitionKey(userId, gameId)).build(),
                        ":From", AttributeValue.builder().n(Long.toString(from.toEpochMilli())).build(),
                        ":To", AttributeValue.builder().n(Long.toString(to.toEpochMilli())).build()))
                .scanIndexForward(false) // Most recent first
                .build());

        return response.items().stream()
                .map(this::mapToRating);
    }

    @Override
    public Optional<Rating> findByTable(User.Id userId, Table.Id tableId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(TABLE_ID_USER_ID_INDEX)
                .keyConditionExpression("UserId = :UserId AND TableId = :TableId")
                .expressionAttributeValues(Map.of(
                        ":UserId", AttributeValue.builder().s(userId.getId()).build(),
                        ":TableId", AttributeValue.builder().s(tableId.getId()).build()))
                .limit(1)
                .build());

        if (!response.hasItems() || response.count() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapToRating(response.items().get(0)));
    }

    @Override
    public Rating findLatest(User.Id userId, Game.Id gameId, Instant before) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("UserIdGameId = :UserIdGameId and #Timestamp < :Before")
                .expressionAttributeNames(Map.of(
                        "#Timestamp", "Timestamp"
                ))
                .expressionAttributeValues(Map.of(
                        ":UserIdGameId", AttributeValue.builder().s(partitionKey(userId, gameId)).build(),
                        ":Before", AttributeValue.builder().n(Long.toString(before.toEpochMilli())).build()
                ))
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

    public void delete(Rating rating) {
        dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "UserIdGameId", AttributeValue.builder().s(partitionKey(rating.getUserId(), rating.getGameId())).build(),
                        "Timestamp", AttributeValue.builder().n(Long.toString(rating.getTimestamp().toEpochMilli())).build()))
                .build());
    }

    @Override
    public Stream<Ranking> findRanking(Game.Id gameId, int maxResults) {
        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(rankingTableName)
                .indexName(GAME_ID_RANK_ORDER_INDEX)
                .keyConditionExpression("GameId = :GameId")
                .expressionAttributeValues(Map.of(":GameId", AttributeValue.builder().s(gameId.getId()).build()))
                .scanIndexForward(false) // Descending, best player first
                .limit(maxResults)
                .build())
                .items()
                .stream()
                .limit(maxResults)
                .map(this::mapToRanking);
    }

    private Ranking mapToRanking(Map<String, AttributeValue> item) {
        return Ranking.builder()
                .gameId(Game.Id.of(item.get("GameId").s()))
                .userId(User.Id.of(item.get("UserId").s()))
                .rating(rankOrderToRating(item.get("RankOrder").s()))
                .build();
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
                .rating(Math.round(Float.parseFloat(attributeValues.get("Rating").n())))
                .deltas(attributeValues.containsKey("Deltas") ? mapToDeltas(attributeValues.get("Deltas").m()) : Collections.emptyMap())
                .build();
    }

    private Map<User.Id, Integer> mapToDeltas(Map<String, AttributeValue> attributeValues) {
        return attributeValues.entrySet().stream()
                .collect(Collectors.toMap(entry -> User.Id.of(entry.getKey()), entry -> Math.round(Float.parseFloat(entry.getValue().n()))));
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

    private AttributeValue mapFromDeltas(Map<User.Id, Integer> deltas) {
        return AttributeValue.builder()
                .m(deltas.entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().getId(),
                        entry -> AttributeValue.builder().n(Integer.toString(entry.getValue())).build())))
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
        return String.format(Locale.ENGLISH, "%010d %019d %s",
                rating.getRating(),
                rating.getTimestamp().toEpochMilli(),
                rating.getUserId().getId());
    }

    private int rankOrderToRating(String rankOrder) {
        return Math.round(Float.parseFloat(rankOrder.split(" ")[0]));
    }
}
