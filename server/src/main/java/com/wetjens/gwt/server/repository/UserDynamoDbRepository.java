package com.wetjens.gwt.server.repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.NotFoundException;

import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@ApplicationScoped
public class UserDynamoDbRepository implements Users {

    private static final String TABLE_NAME = "gwt-users";

    private static final Jsonb JSONB = JsonbBuilder.create();

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public UserDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfig config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix();
    }

    @Override
    public User findById(User.Id id) {
        return findOptionallyById(id).orElseThrow(() -> new NotFoundException("User not found: " + id.getId()));
    }

    @Override
    public void add(User user) {
        var item = new HashMap<>(key(user.getId()));
        item.put("Username", AttributeValue.builder().s(user.getUsername()).build());
        item.put("Email", AttributeValue.builder().s(user.getEmail()).build());
        item.put("Created", AttributeValue.builder().n(Long.toString(user.getCreated().getEpochSecond())).build());
        item.put("Updated", AttributeValue.builder().n(Long.toString(user.getUpdated().getEpochSecond())).build());
        item.put("LastSeen", AttributeValue.builder().n(Long.toString(user.getLastSeen().getEpochSecond())).build());
        item.put("Expires", AttributeValue.builder().n(Long.toString(user.getExpires().getEpochSecond())).build());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    @Override
    public void update(User user) {
        var updates = new HashMap<String, AttributeValueUpdate>();

        updates.put("Username", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().s(user.getUsername()).build())
                .build());

        updates.put("Email", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().s(user.getEmail()).build())
                .build());

        updates.put("Created", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().n(Long.toString(user.getCreated().getEpochSecond())).build())
                .build());

        updates.put("Updated", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().n(Long.toString(user.getUpdated().getEpochSecond())).build())
                .build());

        updates.put("LastSeen", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().n(Long.toString(user.getLastSeen().getEpochSecond())).build())
                .build());

        updates.put("Expires", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().n(Long.toString(user.getExpires().getEpochSecond())).build())
                .build());

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(user.getId()))
                .attributeUpdates(updates)
                .build());
    }

    @Override
    public void updateLastSeen(User.Id id, Instant lastSeen) {
        var updates = new HashMap<String, AttributeValueUpdate>();

        updates.put("LastSeen", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().n(Long.toString(lastSeen.getEpochSecond())).build())
                .build());

        updates.put("Expires", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().n(Long.toString(User.calculateExpires(lastSeen).getEpochSecond())).build())
                .build());

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .attributeUpdates(updates)
                .build());
    }

    @Override
    public Optional<User> findOptionallyById(User.Id id) {
        return getItem(key(id));
    }

    private Optional<User> getItem(Map<String, AttributeValue> key) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .consistentRead(true)
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }

        var item = response.item();

        return Optional.of(User.builder()
                .id(User.Id.of(item.get("Id").s()))
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
                .lastSeen(Instant.ofEpochSecond(Long.parseLong(item.get("LastSeen").n())))
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .username(item.get("Username").s())
                .email(item.get("Email").s())
                .build());
    }

    private Map<String, AttributeValue> key(User.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        return key;
    }
}
