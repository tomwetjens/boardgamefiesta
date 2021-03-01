package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@ApplicationScoped
public class UserDynamoDbRepository implements Users {

    private static final String TABLE_NAME = "gwt-users";
    private static final String USERNAME_INDEX = "Username-index";
    private static final String PREFERRED_USERNAME_INDEX = "PreferredUsername-index";
    private static final String EMAIL_INDEX = "Email-index";
    private static final int FIRST_VERSION = 1;

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Inject
    public UserDynamoDbRepository(@NonNull DynamoDbClient dynamoDbClient, @NonNull DynamoDbConfiguration config) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public void validateBeforeAdd(String email) {
        findByEmail(email).ifPresent(user -> {
            throw new EmailAlreadyInUse();
        });
    }

    @Override
    public User findById(User.Id id, boolean consistentRead) {
        return getItem(key(id), consistentRead).orElseThrow(NotFoundException::new);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(PREFERRED_USERNAME_INDEX)
                .keyConditionExpression("PreferredUsername = :PreferredUsername")
                .expressionAttributeValues(Collections.singletonMap(":PreferredUsername", AttributeValue.builder().s(username).build()))
                .build());

        if (!response.hasItems() || response.count() == 0) {
            return Optional.empty();
        }

        return response
                .items().stream()
                .findAny()
                .map(this::mapToUser);
    }

    @Override
    public Optional<User> findByCognitoUsername(String cognitoUsername) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USERNAME_INDEX)
                .keyConditionExpression("Username = :Username")
                .expressionAttributeValues(Collections.singletonMap(":Username", AttributeValue.builder().s(cognitoUsername.toLowerCase()).build()))
                .build());

        if (!response.hasItems() || response.count() == 0) {
            return Optional.empty();
        }

        return response
                .items().stream()
                .findAny()
                .map(this::mapToUser);
    }

    @Override
    public Stream<User> findByUsernameStartsWith(String username, int maxResults) {
        var itemsByPreferredUsername = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(tableName)
                .indexName(PREFERRED_USERNAME_INDEX)
                .filterExpression("begins_with(PreferredUsername, :PreferredUsername)")
                .expressionAttributeValues(Collections.singletonMap(":PreferredUsername", AttributeValue.builder().s(username).build()))
                .limit(maxResults)
                .build())
                .items();

        // For backwards compatibility, also search the Cognito username
        var itemsByCognitoUsername = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(tableName)
                .indexName(USERNAME_INDEX)
                .filterExpression("begins_with(Username, :Username)")
                .expressionAttributeValues(Collections.singletonMap(":Username", AttributeValue.builder().s(username.toLowerCase()).build()))
                .limit(maxResults)
                .build())
                .items();

        // Combine, sort, then limit and return
        return Stream.concat(itemsByPreferredUsername.stream(), itemsByCognitoUsername.stream())
                .limit(maxResults)
                .map(this::mapToUser)
                .sorted(Comparator.comparing(User::getUsername));
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
        validateBeforeAdd(user.getEmail());

        var item = new HashMap<>(key(user.getId()));
        item.put("Version", AttributeValue.builder().n(user.getVersion().toString()).build());
        item.put("Username", AttributeValue.builder().s(user.getCognitoUsername().toLowerCase()).build());
        item.put("PreferredUsername", AttributeValue.builder().s(user.getUsername()).build());
        item.put("Email", AttributeValue.builder().s(user.getEmail()).build());
        item.put("Created", AttributeValue.builder().n(Long.toString(user.getCreated().getEpochSecond())).build());
        item.put("Updated", AttributeValue.builder().n(Long.toString(user.getUpdated().getEpochSecond())).build());
        item.put("LastSeen", AttributeValue.builder().n(Long.toString(user.getLastSeen().getEpochSecond())).build());
        item.put("Expires", AttributeValue.builder().n(Long.toString(user.getExpires().getEpochSecond())).build());
        item.put("Language", AttributeValue.builder().s(user.getLanguage()).build());
        item.put("Location", user.getLocation().map(location -> AttributeValue.builder().s(location).build()).orElse(null));
        item.put("TimeZone", AttributeValue.builder().s(user.getTimeZone().getId()).build());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    @Override
    public void update(User user) throws ConcurrentModificationException {
        findByEmail(user.getEmail())
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new EmailAlreadyInUse();
                });

        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":Version", AttributeValue.builder().n(Integer.toString(user.getVersion() != null ? user.getVersion() + 1 : FIRST_VERSION)).build());
        expressionAttributeValues.put(":Username", AttributeValue.builder().s(user.getCognitoUsername().toLowerCase()).build());
        expressionAttributeValues.put(":PreferredUsername", AttributeValue.builder().s(user.getUsername()).build());
        expressionAttributeValues.put(":Email", AttributeValue.builder().s(user.getEmail().toLowerCase()).build());
        expressionAttributeValues.put(":Created", AttributeValue.builder().n(Long.toString(user.getCreated().getEpochSecond())).build());
        expressionAttributeValues.put(":Updated", AttributeValue.builder().n(Long.toString(user.getUpdated().getEpochSecond())).build());
        expressionAttributeValues.put(":LastSeen", AttributeValue.builder().n(Long.toString(user.getLastSeen().getEpochSecond())).build());
        expressionAttributeValues.put(":Expires", AttributeValue.builder().n(Long.toString(user.getExpires().getEpochSecond())).build());
        expressionAttributeValues.put(":Language", AttributeValue.builder().s(user.getLanguage()).build());
        expressionAttributeValues.put(":Location", user.getLocation()
                .map(location -> AttributeValue.builder().s(location).build())
                .orElse(AttributeValue.builder().nul(true).build()));
        expressionAttributeValues.put(":TimeZone", AttributeValue.builder().s(user.getTimeZone().getId()).build());

        var builder = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(user.getId()));

        if (user.getVersion() != null) {
            builder = builder.conditionExpression("Version=:ExpectedVersion");
            expressionAttributeValues.put(":ExpectedVersion", AttributeValue.builder().n(user.getVersion().toString()).build());
        }

        var request = builder
                .updateExpression("SET Version=:Version" +
                        ",Username=:Username" +
                        ",PreferredUsername=:PreferredUsername" +
                        ",Email=:Email" +
                        ",Created=:Created" +
                        ",Updated=:Updated" +
                        ",LastSeen=:LastSeen" +
                        ",Expires=:Expires" +
                        ",#Language=:Language" +
                        ",#Location=:Location" +
                        ",#TimeZone=:TimeZone")
                .expressionAttributeNames(Map.of(
                        "#Language", "Language",
                        "#Location", "Location",
                        "#TimeZone", "TimeZone"
                ))
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        try {
            dynamoDbClient.updateItem(request);
        } catch (ConditionalCheckFailedException e) {
            throw new ConcurrentModificationException(e);
        }
    }

    @Override
    public Optional<User> findOptionallyById(User.Id id) {
        return getItem(key(id), false);
    }

    private Optional<User> getItem(Map<String, AttributeValue> key, boolean consistentRead) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .consistentRead(consistentRead)
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(mapToUser(response.item()));
    }

    private User mapToUser(Map<String, AttributeValue> item) {
        return User.builder()
                .id(User.Id.of(item.get("Id").s()))
                .version(item.get("Version") != null ? Integer.valueOf(item.get("Version").n()) : null)
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
                .lastSeen(Instant.ofEpochSecond(Long.parseLong(item.get("LastSeen").n())))
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .cognitoUsername(item.get("Username").s())
                .username(item.get("PreferredUsername") != null ? item.get("PreferredUsername").s() : item.get("Username").s())
                .email(item.get("Email").s())
                .language(item.get("Language") != null ? item.get("Language").s() : User.DEFAULT_LANGUAGE)
                .location(item.get("Location") != null ? item.get("Location").s() : null)
                .timeZone(item.get("TimeZone") != null ? ZoneId.of(item.get("TimeZone").s()) : null)
                .build();
    }

    private Map<String, AttributeValue> key(User.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        return key;
    }
}
