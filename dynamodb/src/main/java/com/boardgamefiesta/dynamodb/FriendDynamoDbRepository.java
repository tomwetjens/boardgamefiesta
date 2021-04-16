package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.domain.user.Friends;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class FriendDynamoDbRepository implements Friends {

    private static final String TABLE_NAME = "gwt-friends";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public FriendDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Optional<Friend> findById(Friend.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "UserId", AttributeValue.builder().s(id.getUserId().getId()).build(),
                        "OtherUserId", AttributeValue.builder().s(id.getOtherUserId().getId()).build()))
                .build());

        if (!response.hasItem() || response.item() == null) {
            return Optional.empty();
        }

        return Optional.of(mapItemToFriend(response.item()));
    }

    @Override
    public Stream<Friend> findByUserId(User.Id userId, int maxResults) {
        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("UserId = :UserId")
                .expressionAttributeValues(Map.of(":UserId", AttributeValue.builder().s(userId.getId()).build()))
                .limit(maxResults)
                .build())
                .items()
                .stream()
                .limit(maxResults)
                .map(this::mapItemToFriend);
    }

    @Override
    public void add(Friend friend) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(mapFriendToItem(friend))
                .build());
    }

    @Override
    public void update(Friend friend) {
        if (friend.isEnded()) {
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "UserId", AttributeValue.builder().s(friend.getId().getUserId().getId()).build(),
                            "OtherUserId", AttributeValue.builder().s(friend.getId().getOtherUserId().getId()).build()))
                    .build());
        }
    }

    private Map<String, AttributeValue> mapFriendToItem(Friend friend) {
        return Map.of(
                "UserId", AttributeValue.builder().s(friend.getId().getUserId().getId()).build(),
                "OtherUserId", AttributeValue.builder().s(friend.getId().getOtherUserId().getId()).build(),
                "Started", AttributeValue.builder().s(friend.getStarted().toString()).build());
    }

    public Friend mapItemToFriend(Map<String, AttributeValue> item) {
        return Friend.builder()
                .id(Friend.Id.of(
                        User.Id.of(item.get("UserId").s()),
                        User.Id.of(item.get("OtherUserId").s())))
                .started(Instant.parse(item.get("Started").s()))
                .build();
    }
}
