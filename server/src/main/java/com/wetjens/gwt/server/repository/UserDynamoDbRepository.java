package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.NotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
                .value(AttributeValue.builder().s(user.getUsername())
                        .build())
                .build());

        updates.put("Email", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().s(user.getEmail())
                        .build())
                .build());

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(user.getId()))
                .attributeUpdates(updates)
                .build());
    }

    @Override
    public Optional<User> findOptionallyById(User.Id id) {
        return getItem(key(id));
    }

    private Optional<User> getItem(Map<String, AttributeValue> key) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .consistentRead(true)
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }

        Map<String, AttributeValue> item = response.item();

        return Optional.of(User.builder()
                .id(User.Id.of(item.get("Id").s()))
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
