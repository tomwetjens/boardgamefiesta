package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

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
    public User get(User.Id id) {
        return getItem(key(id)).orElseGet(() -> {
            User user = User.createAutomatically(id);
            add(user);
            return user;
        });
    }

    private void add(User user) {

    }

    @Override
    public User findById(User.Id id) {
        return getItem(key(id))
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
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

        return Optional.of(deserialize(response.item().get("Data").s()));
    }

    private Map<String, AttributeValue> key(User.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        return key;
    }

    private AttributeValue serialize(User user) {
        return AttributeValue.builder().s(JSONB.toJson(user)).build();
    }

    private User deserialize(String data) {
        return JSONB.fromJson(data, User.class);
    }

}
