package com.wetjens.gwt.server.user.repository;

import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.user.domain.User;
import com.wetjens.gwt.server.user.domain.Users;
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
public class UserRepository implements Users {

    private static final String TABLE_NAME = "gwt-users";

    private static final Jsonb JSONB = JsonbBuilder.create();

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public UserRepository(@NonNull DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + getTableNameSuffix();
    }

    private String getTableNameSuffix() {
        return "prod".equalsIgnoreCase(System.getenv("ENV")) ? "" : "-test";
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
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }

        return Optional.of(deserialize(response.item().get("Data").s()));
    }

    private Map<String, AttributeValue> key(User.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s("User-" + id.getId()).build());
        return key;
    }

    private AttributeValue serialize(User user) {
        return AttributeValue.builder().s(JSONB.toJson(user)).build();
    }

    private User deserialize(String data) {
        return JSONB.fromJson(data, User.class);
    }

}
