package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.event.WebSocketConnection;
import com.boardgamefiesta.domain.event.WebSocketConnections;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import org.w3c.dom.Attr;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class WebSocketConnectionDynamoDbRepository implements WebSocketConnections {

    private static final String TABLE_NAME = "gwt-ws-connections";
    private static final String USER_ID_INDEX = "UserId-index";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public WebSocketConnectionDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient,
                                                 @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public void add(WebSocketConnection webSocketConnection) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "Id", AttributeValue.builder().s(webSocketConnection.getId()).build(),
                        "UserId", AttributeValue.builder().s(webSocketConnection.getUserId().getId()).build(),
                        "Status", AttributeValue.builder().s(webSocketConnection.getStatus().name()).build(),
                        "Created", AttributeValue.builder().n(Long.toString(webSocketConnection.getCreated().getEpochSecond())).build(),
                        "Updated", AttributeValue.builder().n(Long.toString(webSocketConnection.getUpdated().getEpochSecond())).build(),
                        "Expires", AttributeValue.builder().n(Long.toString(webSocketConnection.getExpires().getEpochSecond())).build()
                ))
                .build());
    }

    public WebSocketConnection mapFromItem(Map<String, AttributeValue> item) {
        return WebSocketConnection.builder()
                .id(item.get("Id").s())
                .userId(User.Id.of(item.containsKey("UserId") ? item.get("UserId").s() : null))
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
                .status(WebSocketConnection.Status.valueOf(item.get("Status").s()))
                .build();
    }

    @Override
    public void remove(String id) {
        dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("Id", AttributeValue.builder().s(id).build()))
                .build());
    }

    @Override
    public void updateStatus(String id, User.Id userId, Instant updated, WebSocketConnection.Status status) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("Id", AttributeValue.builder().s(id).build()))
                .updateExpression("SET #Status=:Status, #Updated=:Updated, #Expires=:Expires")
                .expressionAttributeNames(Map.of(
                        "#Status", "Status",
                        "#Updated", "Updated",
                        "#Expires", "Expires"
                ))
                .expressionAttributeValues(Map.of(
                        ":Status", AttributeValue.builder().s(status.name()).build(),
                        ":Updated", AttributeValue.builder().n(Long.toString(updated.getEpochSecond())).build(),
                        ":Expires", AttributeValue.builder().n(Long.toString(WebSocketConnection.calculateExpires(updated).getEpochSecond())).build()
                ))
                .build());
    }

    @Override
    public boolean wasActiveAfter(User.Id userId, Instant after) {
        return dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .filterExpression("#Status = :Active AND #Updated >= :After")
                .expressionAttributeNames(Map.of(
                        "#Status", "Status",
                        "#Updated", "Updated"
                ))
                .expressionAttributeValues(Map.of(
                        ":UserId", AttributeValue.builder().s(userId.getId()).build(),
                        ":Active", AttributeValue.builder().s(WebSocketConnection.Status.ACTIVE.name()).build(),
                        ":After", AttributeValue.builder().n(Long.toString(after.getEpochSecond())).build()
                ))
                .limit(1)
                .select(Select.COUNT)
                .build())
                .count() > 0;
    }
}
