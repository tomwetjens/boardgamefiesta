/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.domain.user.Friends;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * PK=User#<UserID>
 * SK=Friend#<OtherUserID>
 */
@ApplicationScoped
@Slf4j
public class FriendDynamoDbRepositoryV2 implements Friends {

    private static final String PK = "PK";
    private static final String SK = "SK";

    private static final String USER_PREFIX = "User#";
    private static final String FRIEND_PREFIX = "Friend#";

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public FriendDynamoDbRepositoryV2(@NonNull DynamoDbClient client,
                                      @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Optional<Friend> findById(Friend.Id id) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.tableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + "=:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + id.getUserId().getId()),
                        ":SK", Item.s(FRIEND_PREFIX + id.getOtherUserId().getId())
                ))
                .build());

        if (response.hasItems() && !response.items().isEmpty()) {
            return Optional.of(mapToFriend(Item.of(response.items().get(0))));
        }
        return Optional.empty();
    }

    private Friend mapToFriend(Item item) {
        return Friend.builder()
                .id(Friend.Id.of(
                        User.Id.of(item.getString("PK").replace(USER_PREFIX, "")),
                        User.Id.of(item.getString("SK").replace(FRIEND_PREFIX, ""))))
                .started(item.getInstant("Started"))
                .build();
    }

    @Override
    public Stream<Friend> findByUserId(User.Id userId, int maxResults) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.tableName())
                .keyConditionExpression(PK + "=:PK AND begins_with(" + SK + ",:SK)")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + userId.getId()),
                        ":SK", Item.s(FRIEND_PREFIX)))
                .limit(maxResults)
                .build())
                .items().stream()
                .map(Item::of)
                .map(this::mapToFriend);
    }

    @Override
    public void add(Friend friend) {
        client.putItem(PutItemRequest.builder()
                .tableName(config.tableName())
                .item(new Item()
                        .setString(PK, USER_PREFIX + friend.getId().getUserId().getId())
                        .setString(SK, FRIEND_PREFIX + friend.getId().getOtherUserId().getId())
                        .setInstant("Started", friend.getStarted())
                        .asMap())
                .build());
    }

    @Override
    public void update(Friend friend) {
        if (friend.isEnded()) {
            delete(friend.getId());
        }
    }

    public void delete(Friend.Id friendId) {
        client.deleteItem(DeleteItemRequest.builder()
                .tableName(config.tableName())
                .key(Map.of(
                        PK, Item.s(USER_PREFIX + friendId.getUserId().getId()),
                        SK, Item.s(FRIEND_PREFIX + friendId.getOtherUserId().getId())))
                .build());
    }
}
