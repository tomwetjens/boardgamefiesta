package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.rating.Ranking;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Entity type: Rating
 * Ratings per user per game:
 * PK=User#<UserID>
 * SK=Rating#<GameID>#<Timestamp>
 * <p>
 * Ratings per table:
 * GSI1PK=Table#<TableID>
 * GSI1SK=Rating#<UserID>
 * <p>
 * <p>
 * Entity type: Ranking
 * PK=User#<UserID>
 * SK=Rating#<GameID>
 * Ranking per game:
 * GSI1PK=Game#<GameID>#<Shard>
 * GSI1SK=<Rating>#<Timestamp>#<UserID>
 */
@ApplicationScoped
@Slf4j
public class RatingDynamoDbRepositoryV2 implements Ratings {

    private static final String PK = "PK";
    private static final String SK = "SK";

    private static final String GSI1 = "GSI1";
    private static final String GSI1PK = "GSI1PK";
    private static final String GSI1SK = "GSI1SK";

    private static final String GAME_PREFIX = "Game#";
    private static final String USER_PREFIX = "User#";
    private static final String TABLE_PREFIX = "Table#";
    private static final String RATING_PREFIX = "Rating#";
    private static final String RANKING_PREFIX = "Ranking#";

    private static final DateTimeFormatter TIMESTAMP_SECS_FORMATTER = new DateTimeFormatterBuilder()
            .parseStrict()
            .appendInstant(0) // No fractional second
            .toFormatter();

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public RatingDynamoDbRepositoryV2(@NonNull DynamoDbClient client,
                                      @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Stream<Rating> findHistoric(User.Id userId, Game.Id gameId, Instant from, Instant to) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + " BETWEEN :From AND :To")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + userId.getId()),
                        ":From", Item.s(RATING_PREFIX + gameId.getId() + "#" + from),
                        ":To", Item.s(RATING_PREFIX + gameId.getId() + "#" + to)
                ))
                .scanIndexForward(false)
                .build())
                .items().stream()
                .map(Item::of)
                .map(this::mapToRating);
    }

    private Rating mapToRating(Item item) {
        var pk = item.getString(PK).split("#");
        var sk = item.getString(SK).split("#");
        return Rating.builder()
                .userId(User.Id.of(pk[1]))
                .gameId(Game.Id.of(sk[1]))
                .tableId(item.getOptionalString(GSI1PK)
                        .map(gsi1pk -> Table.Id.of(gsi1pk.replace(TABLE_PREFIX, "")))
                        .orElse(null))
                .timestamp(Instant.parse(sk[2]))
                .rating(item.getInt("Rating"))
                .deltas(mapToDeltas(item.getMap("Deltas")))
                .build();
    }

    private Map<User.Id, Integer> mapToDeltas(Map<String, AttributeValue> attributeValues) {
        return attributeValues.entrySet().stream()
                .collect(Collectors.toMap(entry -> User.Id.of(entry.getKey()), entry -> Math.round(Float.parseFloat(entry.getValue().n()))));
    }

    private AttributeValue mapFromDeltas(Map<User.Id, Integer> deltas) {
        return AttributeValue.builder()
                .m(deltas.entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().getId(),
                        entry -> AttributeValue.builder().n(Integer.toString(entry.getValue())).build())))
                .build();
    }

    @Override
    public Optional<Rating> findByTable(User.Id userId, Table.Id tableId) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI1)
                .keyConditionExpression(GSI1PK + "=:PK AND " + GSI1SK + "=:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(TABLE_PREFIX + tableId.getId()),
                        ":SK", Item.s(RATING_PREFIX + userId.getId())
                ))
                .build());

        if (response.hasItems() && !response.items().isEmpty()) {
            var item = response.items().get(0);
            return Optional.of(mapToRating(Item.of(client.getItem(GetItemRequest.builder()
                    .tableName(config.getTableName())
                    .key(Map.of(PK, item.get(PK), SK, item.get(SK)))
                    .build())
                    .item())));
        }
        return Optional.empty();
    }

    @Override
    public Rating findLatest(User.Id userId, Game.Id gameId, Instant before) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + " BETWEEN :From AND :To")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + userId.getId()),
                        ":From", Item.s(RATING_PREFIX + gameId.getId() + "#"),
                        ":To", Item.s(RATING_PREFIX + gameId.getId() + "#" + before)
                ))
                .scanIndexForward(false)
                .limit(1)
                .build())
                .items().stream()
                .findFirst()
                .map(Item::of)
                .map(this::mapToRating)
                .orElse(Rating.initial(userId, gameId));
    }

    @Override
    public void addAll(Collection<Rating> ratings) {
        client.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(config.getTableName(),
                        ratings.stream()
                                .flatMap(rating -> Stream.of(
                                        WriteRequest.builder()
                                                .putRequest(PutRequest.builder()
                                                        .item(mapFromRating(rating).asMap())
                                                        .build())
                                                .build(),
                                        WriteRequest.builder()
                                                .putRequest(PutRequest.builder()
                                                        .item(new Item()
                                                                .setString(PK, USER_PREFIX + rating.getUserId().getId())
                                                                .setString(SK, RATING_PREFIX + rating.getGameId().getId())
                                                                .setString(GSI1PK, GAME_PREFIX + rating.getGameId().getId() + "#" + Math.abs(rating.getTimestamp().hashCode()) % config.getWriteGameIdShards())
                                                                // 1. By rating, with leading zeros (because of lexicographical sorting)
                                                                // 2. Then by timestamp, in case 2 users have the same rating
                                                                // 3. Then by user id, to make it guaranteed unique, in case 2 users have the same rating at the same time
                                                                .setString(GSI1SK, RANKING_PREFIX + String.format(Locale.ENGLISH, "%05d#%s#%s",
                                                                        rating.getRating(),
                                                                        TIMESTAMP_SECS_FORMATTER.format(rating.getTimestamp()),
                                                                        rating.getUserId().getId()))
                                                                .asMap())
                                                        .build())
                                                .build()
                                ))
                                .collect(Collectors.toList())))
                .build());
    }

    private Item mapFromRating(Rating rating) {
        var item = new Item()
                .setString(PK, USER_PREFIX + rating.getUserId().getId())
                .setString(SK, RATING_PREFIX + rating.getGameId().getId() + "#" + TIMESTAMP_SECS_FORMATTER.format(rating.getTimestamp()))
                .setInt("Rating", rating.getRating())
                .set("Deltas", mapFromDeltas(rating.getDeltas()));

        rating.getTableId().ifPresent(tableId -> {
            item.setString(GSI1PK, TABLE_PREFIX + tableId.getId());
            item.setString(GSI1SK, RATING_PREFIX + rating.getUserId().getId());
        });

        return item;
    }

    @Override
    public Stream<Ranking> findRanking(Game.Id gameId, int maxResults) {
        // Scatter-gather across shards
        return IntStream.range(0, config.getReadGameIdShards())
                .parallel()
                .mapToObj(shard -> client.queryPaginator(QueryRequest.builder()
                        .tableName(config.getTableName())
                        .indexName(GSI1)
                        .keyConditionExpression(GSI1PK + "=:GSI1PK AND begins_with(" + GSI1SK + ",:GSI1SK)")
                        .expressionAttributeValues(Map.of(
                                ":GSI1PK", Item.s(GAME_PREFIX + gameId.getId() + "#" + shard),
                                ":GSI1SK", Item.s(RANKING_PREFIX)
                        ))
                        .scanIndexForward(false)
                        .limit(maxResults)
                        .build())
                        .stream()
                        .filter(QueryResponse::hasItems)
                        .flatMap(response -> response.items().stream())
                        .map(Item::of)
                        .limit(maxResults))
                .flatMap(Function.identity())
                .sorted(Comparator.<Item, String>comparing(item -> item.getString(GSI1SK)).reversed())
                .map(this::mapToRanking)
                .limit(maxResults);
    }

    private Ranking mapToRanking(Item item) {
        var pk = item.getString(GSI1PK).split("#");
        var sk = item.getString(GSI1SK).split("#");
        return Ranking.builder()
                .gameId(Game.Id.of(pk[1]))
                .rating(Integer.parseInt(sk[1]))
                .userId(User.Id.of(sk[3]))
                .build();
    }
}
