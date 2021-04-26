package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Slf4j
public class UserDynamoDbRepository implements Users {

    private static final String TABLE_NAME = "gwt-users";
    private static final String USERNAME_INDEX = "Username-index";
    private static final String PREFERRED_USERNAME_INDEX = "PreferredUsername-index";
    private static final String EMAIL_INDEX = "Email-index";

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
    public Optional<User> findByUsername(String username) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(PREFERRED_USERNAME_INDEX)
                .keyConditionExpression("PreferredUsername = :PreferredUsername")
                .expressionAttributeValues(Collections.singletonMap(":PreferredUsername", AttributeValue.builder().s(username).build()))
                .build());

        if (!response.hasItems() || response.count() == 0) {
            // For backwards compatibility, also search the Cognito username
            return findFullByCognitoUsername(username);
        }

        return response
                .items().stream()
                .findAny()
                .map(this::mapToUser);
    }

    private Optional<User> findFullByCognitoUsername(String cognitoUsername) {
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
    public Optional<User.Id> findIdByCognitoUsername(String cognitoUsername) {
        return findFullByCognitoUsername(cognitoUsername).map(User::getId);
    }

    @Override
    public Stream<User> findByUsernameStartsWith(String username, int maxResults) {
        var itemsByPreferredUsername = dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(tableName)
                .indexName(PREFERRED_USERNAME_INDEX)
                .filterExpression("begins_with(PreferredUsername, :PreferredUsername)")
                .expressionAttributeValues(Collections.singletonMap(":PreferredUsername", AttributeValue.builder().s(username).build()))
                .build())
                .items()
                .stream()
                .limit(maxResults)
                .collect(Collectors.toList());

        log.info("Found {} users (limit is {}) with preferred username that begins with '{}'", itemsByPreferredUsername.size(), maxResults, username);

        // For backwards compatibility, also search the Cognito username
        var itemsByCognitoUsername = dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(tableName)
                .indexName(USERNAME_INDEX)
                .filterExpression("begins_with(Username, :Username)")
                .expressionAttributeValues(Collections.singletonMap(":Username", AttributeValue.builder().s(username.toLowerCase()).build()))
                .build())
                .items()
                .stream()
                .limit(maxResults)
                .collect(Collectors.toList());

        log.info("Found {} users (limit is {}) with Cognito username that begins with '{}'", itemsByCognitoUsername.size(), maxResults, username);

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
        item.put("Version", AttributeValue.builder().n(Integer.toString(user.getVersion())).build());
        item.put("Username", AttributeValue.builder().s(user.getCognitoUsername().toLowerCase()).build());
        item.put("PreferredUsername", AttributeValue.builder().s(user.getUsername()).build());
        item.put("Email", AttributeValue.builder().s(user.getEmail()).build());
        item.put("Created", AttributeValue.builder().n(Long.toString(user.getCreated().getEpochSecond())).build());
        item.put("Updated", AttributeValue.builder().n(Long.toString(user.getUpdated().getEpochSecond())).build());
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
        expressionAttributeValues.put(":Version", AttributeValue.builder().n(Integer.toString(user.getVersion() + 1)).build());
        expressionAttributeValues.put(":Username", AttributeValue.builder().s(user.getCognitoUsername().toLowerCase()).build());
        expressionAttributeValues.put(":PreferredUsername", AttributeValue.builder().s(user.getUsername()).build());
        expressionAttributeValues.put(":Email", AttributeValue.builder().s(user.getEmail().toLowerCase()).build());
        expressionAttributeValues.put(":Created", AttributeValue.builder().n(Long.toString(user.getCreated().getEpochSecond())).build());
        expressionAttributeValues.put(":Updated", AttributeValue.builder().n(Long.toString(user.getUpdated().getEpochSecond())).build());
        expressionAttributeValues.put(":Language", AttributeValue.builder().s(user.getLanguage()).build());
        expressionAttributeValues.put(":Location", user.getLocation()
                .map(location -> AttributeValue.builder().s(location).build())
                .orElse(AttributeValue.builder().nul(true).build()));
        expressionAttributeValues.put(":TimeZone", AttributeValue.builder().s(user.getTimeZone().getId()).build());

        expressionAttributeValues.put(":ExpectedVersion", AttributeValue.builder().n(Integer.toString(user.getVersion())).build());

        var request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(user.getId()))
                .conditionExpression("Version=:ExpectedVersion")
                .updateExpression("SET Version=:Version" +
                        ",Username=:Username" +
                        ",PreferredUsername=:PreferredUsername" +
                        ",Email=:Email" +
                        ",Created=:Created" +
                        ",Updated=:Updated" +
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
    public Optional<User> findById(User.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(mapToUser(response.item()));
    }

    public User mapToUser(Map<String, AttributeValue> item) {
        return User.builder()
                .id(User.Id.of(item.get("Id").s()))
                .version(item.containsKey("Version") ? Integer.parseInt(item.get("Version").n()) : 1)
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n())))
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
