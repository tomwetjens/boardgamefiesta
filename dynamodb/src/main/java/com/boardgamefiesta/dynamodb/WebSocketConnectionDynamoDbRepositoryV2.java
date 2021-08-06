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

import com.boardgamefiesta.domain.event.WebSocketConnection;
import com.boardgamefiesta.domain.event.WebSocketConnections;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;

/**
 * PK=WebSocket#<ID>
 * SK=WebSocket#<ID>
 * <p>
 * GSI1: by User ID
 * GSI1PK=User#<ID>
 * GSI1SK=WebSocket#<ID>
 * <p>
 * GSI2: by User ID - only when status=Active
 * GSI2PK=User#<ID>
 * GSI2SK=WebSocket#<Updated>#<ID>
 */
@ApplicationScoped
public class WebSocketConnectionDynamoDbRepositoryV2 implements WebSocketConnections {

    private static final String PK = "PK";
    private static final String SK = "SK";
    private static final String TTL = "TTL";

    private static final String GSI1 = "GSI1";
    private static final String GSI1PK = "GSI1PK";
    private static final String GSI1SK = "GSI1SK";

    private static final String GSI2 = "GSI2";
    private static final String GSI2PK = "GSI2PK";
    private static final String GSI2SK = "GSI2SK";

    private static final String USER_PREFIX = "User#";
    private static final String WEB_SOCKET_PREFIX = "WebSocket#";

    private static final DateTimeFormatter TIMESTAMP_SECS_FORMATTER = new DateTimeFormatterBuilder()
            .parseStrict()
            .appendInstant(0) // No fractional second
            .toFormatter();

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public WebSocketConnectionDynamoDbRepositoryV2(@NonNull DynamoDbClient client,
                                                   @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public void add(WebSocketConnection webSocketConnection) {
        var item = new Item()
                .setString(PK, WEB_SOCKET_PREFIX + webSocketConnection.getId())
                .setString(SK, WEB_SOCKET_PREFIX + webSocketConnection.getId())
                .setString(GSI1PK, USER_PREFIX + webSocketConnection.getUserId().getId())
                .setString(GSI1SK, WEB_SOCKET_PREFIX + webSocketConnection.getId())
                .setEnum("Status", webSocketConnection.getStatus())
                .setInstant("Created", webSocketConnection.getCreated())
                .setInstant("Updated", webSocketConnection.getUpdated())
                .setTTL(TTL, webSocketConnection.getExpires());

        if (webSocketConnection.getStatus() == WebSocketConnection.Status.ACTIVE) {
            item.setString(GSI2PK, USER_PREFIX + webSocketConnection.getUserId().getId())
                    .setString(GSI2SK, WEB_SOCKET_PREFIX + webSocketConnection.getUpdated() + "#" + webSocketConnection.getId());
        }

        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(item.asMap())
                .build());
    }

    @Override
    public void remove(String id) {
        client.deleteItem(DeleteItemRequest.builder()
                .tableName(config.getTableName())
                .key(new Item()
                        .setString(PK, WEB_SOCKET_PREFIX + id)
                        .setString(SK, WEB_SOCKET_PREFIX + id)
                        .asMap())
                .build());
    }

    @Override
    public void updateStatus(String id, User.Id userId, Instant updated, WebSocketConnection.Status status) {
        var updateItem = new UpdateItem()
                .setInstant("Updated", updated)
                .setEnum("Status", status)
                .setTTL(TTL, WebSocketConnection.calculateExpires(updated));

        if (status == WebSocketConnection.Status.ACTIVE) {
            updateItem.setString(GSI2PK, USER_PREFIX + userId.getId())
                    .setString(GSI2SK, WEB_SOCKET_PREFIX + TIMESTAMP_SECS_FORMATTER.format(updated) + "#" + id);
        } else {
            updateItem.remove(GSI2PK, GSI2SK);
        }

        client.updateItem(UpdateItemRequest.builder()
                .tableName(config.getTableName())
                .key(new Item()
                        .setString(PK, WEB_SOCKET_PREFIX + id)
                        .setString(SK, WEB_SOCKET_PREFIX + id)
                        .asMap())
                .updateExpression(updateItem.getUpdateExpression())
                .expressionAttributeNames(updateItem.getExpressionAttributeNames())
                .expressionAttributeValues(updateItem.getExpressionAttributeValues())
                .build());
    }

    @Override
    public boolean wasActiveAfter(User.Id userId, Instant after) {
        return client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI2)
                .keyConditionExpression(GSI2PK + "=:GSI2PK AND " + GSI2SK + ">:GSI2SK")
                .expressionAttributeValues(Map.of(
                        ":GSI2PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI2SK", Item.s(WEB_SOCKET_PREFIX + TIMESTAMP_SECS_FORMATTER.format(after))
                ))
                .select(Select.COUNT)
                .build()).count() > 0;
    }
}
