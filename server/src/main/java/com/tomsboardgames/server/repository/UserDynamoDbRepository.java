package com.tomsboardgames.server.repository;

import com.tomsboardgames.server.domain.User;
import com.tomsboardgames.server.domain.Users;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class UserDynamoDbRepository implements Users {

    private static final String TABLE_NAME = "gwt-users";
    private static final String USERNAME_INDEX = "Username-index";
    private static final String EMAIL_INDEX = "Email-index";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public UserDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public User findById(User.Id id) {
        return findOptionallyById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Stream<User> findByUsernameStartsWith(String username) {
        return dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(tableName)
                .indexName(USERNAME_INDEX)
                .filterExpression("begins_with(Username, :Username)")
                .expressionAttributeValues(Collections.singletonMap(":Username", AttributeValue.builder().s(username.toLowerCase()).build()))
                .build())
                .items().stream()
                .map(this::mapToUser);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(EMAIL_INDEX)
                .keyConditionExpression("Email = :Email")
                .expressionAttributeValues(Collections.singletonMap(":Email", AttributeValue.builder().s(email.toLowerCase()).build()))
                .build());

        if (!response.hasItems() || response.count() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapToUser(response.items().get(0)));
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
        item.put("Language", AttributeValue.builder().s(user.getLanguage()).build());

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
                .value(AttributeValue.builder().s(user.getUsername().toLowerCase()).build())
                .build());

        updates.put("Email", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().s(user.getEmail().toLowerCase()).build())
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

        updates.put("Language", AttributeValueUpdate.builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.builder().s(user.getLanguage()).build())
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
        return Optional.of(mapToUser(response.item()));
    }

    private User mapToUser(Map<String, AttributeValue> item) {
        return User.builder()
                .id(User.Id.of(item.get("Id").s()))
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
                .lastSeen(Instant.ofEpochSecond(Long.parseLong(item.get("LastSeen").n())))
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .username(item.get("Username").s())
                .email(item.get("Email").s())
                .language(item.get("Language") != null ? item.get("Language").s() : User.DEFAULT_LANGUAGE)
                .build();
    }

    private Map<String, AttributeValue> key(User.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        return key;
    }
}
