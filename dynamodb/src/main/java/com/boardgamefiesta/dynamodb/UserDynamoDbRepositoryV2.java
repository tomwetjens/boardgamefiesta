package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.user.EmailPreferences;
import com.boardgamefiesta.domain.user.TurnBasedPreferences;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PK=User#<ID>
 * SK=User#<ID>
 * <p>
 * GSI1: case-insensitive search by username
 * GSI1PK=User#<first_3_letters_of_username_lowercase> (sharding)
 * GSI1SK=User#<username_lowercase>
 * <p>
 * GSI2: search by e-mail address (exact)
 * GSI2PK=User#<email_lowercase>
 * GSI2SK=User#<email_lowercase>
 * <p>
 * GSI3: by Cognito username (exact)
 * GSI3PK=User#<cognito:username>
 * GSI3SK=User#<cognito:username>
 */
@ApplicationScoped
@Slf4j
public class UserDynamoDbRepositoryV2 implements Users {

    private static final String PK = "PK";
    private static final String SK = "SK";

    private static final String GSI1 = "GSI1";
    private static final String GSI1PK = "GSI1PK";
    private static final String GSI1SK = "GSI1SK";

    private static final String GSI2 = "GSI2";
    private static final String GSI2PK = "GSI2PK";
    private static final String GSI2SK = "GSI2SK";

    private static final String GSI3 = "GSI3";
    private static final String GSI3PK = "GSI3PK";
    private static final String GSI3SK = "GSI3SK";

    private static final String USER_PREFIX = "User#";

    private static final String VERSION = "Version";

    private static final int MAX_BATCH_GET_ITEM_SIZE = 100;

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public UserDynamoDbRepositoryV2(@NonNull DynamoDbClient client,
                                    @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Optional<User> findById(User.Id id) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + "=:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + id.getId()),
                        ":SK", Item.s(USER_PREFIX + id.getId())
                ))
                .build());

        if (response.hasItems() && !response.items().isEmpty()) {
            return Optional.of(mapToUser(Item.of(response.items().get(0))));
        }
        return Optional.empty();
    }

    @Override
    public Stream<User> findByIds(Stream<User.Id> ids) {
        return Chunked.stream(ids, MAX_BATCH_GET_ITEM_SIZE)
                .map(chunk -> chunk
                        .map(id -> Map.of(
                                PK, Item.s(USER_PREFIX + id.getId()),
                                SK, Item.s(USER_PREFIX + id.getId())))
                        .collect(Collectors.toList()))
                .flatMap(keys -> {
                    var response = client.batchGetItem(BatchGetItemRequest.builder()
                            .requestItems(Map.of(config.getTableName(),
                                    KeysAndAttributes.builder()
                                            .keys(keys)
                                            .build()))
                            .build());

                    if (response.hasResponses()) {
                        var items = response.responses().get(config.getTableName()).stream()
                                .collect(Collectors.toMap(item -> item.get(PK).s(), Function.identity()));
                        return keys.stream()
                                .map(key -> key.get(PK).s())
                                .map(items::get)
                                .map(Item::of)
                                .map(this::mapToUser);
                    }
                    return Stream.empty();
                });
    }

    private User mapToUser(Item item) {
        return User.builder()
                .id(User.Id.of(item.getString(PK).replace(USER_PREFIX, "")))
                .version(item.getInt(VERSION))
                .username(item.getString("Username"))
                .cognitoUsername(item.getString("CognitoUsername"))
                .email(item.getString("Email"))
                .language(item.getString("Language"))
                .location(item.getString("Location"))
                .timeZone(item.getOptionalString("TimeZone").map(ZoneId::of).orElse(null))
                .created(item.getInstant("Created"))
                .updated(item.getInstant("Updated"))
                .emailPreferences(item.getOptionalMap("EmailPreferences")
                        .map(Item::of)
                        .map(this::mapToEmailPreferences)
                        .orElseGet(EmailPreferences::new))
                .deleted(item.getOptionalBoolean("Deleted").orElse(false))
                .build();
    }

    private EmailPreferences mapToEmailPreferences(Item attributeValue) {
        return EmailPreferences.builder()
                .sendInviteEmail(attributeValue.getBoolean("SendInviteEmail"))
                .turnBasedPreferences(mapToTurnBasedPreferences(Item.of(attributeValue.getMap("TurnBasedPreferences"))))
                .build();
    }

    private TurnBasedPreferences mapToTurnBasedPreferences(Item attributeValue) {
        return TurnBasedPreferences.builder()
                .sendTurnEmail(attributeValue.getBoolean("SendTurnEmail"))
                .sendEndedEmail(attributeValue.getBoolean("SendEndedEmail"))
                .build();
    }

    @Override
    public Stream<User> findByUsernameStartsWith(String username, int maxResults) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI1)
                .keyConditionExpression(GSI1PK + "=:PK AND begins_with(" + GSI1SK + ",:SK)")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + username.substring(0, 3).toLowerCase()),
                        ":SK", Item.s(USER_PREFIX + username.toLowerCase())
                ))
                .limit(maxResults)
                .build())
                .items().stream()
                .map(item -> User.Id.of(item.get(PK).s().replace(USER_PREFIX, "")))
                .flatMap(id -> this.findById(id).stream());
    }

    @Override
    public Optional<User> findByUsername(String username) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI1)
                .keyConditionExpression(GSI1PK + "=:PK AND " + GSI1SK + "=:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + username.substring(0, 3).toLowerCase()),
                        ":SK", Item.s(USER_PREFIX + username.toLowerCase())
                ))
                .build());

        if (response.hasItems() && !response.items().isEmpty()) {
            var item = response.items().get(0);
            var userId = User.Id.of(item.get(PK).s().replace(USER_PREFIX, ""));
            return this.findById(userId);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI2)
                .keyConditionExpression(GSI2PK + "=:PK AND " + GSI2SK + "=:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + email.toLowerCase()),
                        ":SK", Item.s(USER_PREFIX + email.toLowerCase())
                ))
                .build());

        if (response.hasItems() && !response.items().isEmpty()) {
            var item = response.items().get(0);
            var userId = User.Id.of(item.get(PK).s().replace(USER_PREFIX, ""));
            return this.findById(userId);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User.Id> findIdByCognitoUsername(String cognitoUsername) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI3)
                .keyConditionExpression(GSI3PK + "=:PK AND " + GSI3SK + "=:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + cognitoUsername),
                        ":SK", Item.s(USER_PREFIX + cognitoUsername)
                ))
                .build());

        if (response.hasItems() && !response.items().isEmpty()) {
            var item = response.items().get(0);
            var userId = User.Id.of(item.get(PK).s().replace(USER_PREFIX, ""));
            return Optional.of(userId);
        }
        return Optional.empty();
    }

    @Override
    public void add(User user) {
        validateBeforeAdd(user.getEmail());

        put(user);
    }

    public void put(User user) {
        var item = new Item()
                .setString(PK, USER_PREFIX + user.getId().getId())
                .setString(SK, USER_PREFIX + user.getId().getId())
                .setInt(VERSION, user.getVersion())
                .setString("Username", user.getUsername())
                .setString("CognitoUsername", user.getCognitoUsername())
                .setString("Email", user.getEmail())
                .setString("Language", user.getLanguage())
                .setString("Location", user.getLocation().orElse(null))
                .setString("TimeZone", user.getTimeZone().getId())
                .setInstant("Created", user.getCreated())
                .setInstant("Updated", user.getUpdated())
                .setBoolean("Deleted", user.isDeleted())
                .set("EmailPreferences", mapFromEmailPreferences(user.getEmailPreferences()));

        if (!user.isDeleted()) {
            item.setString(GSI1PK, USER_PREFIX + user.getUsername().substring(0, 3).toLowerCase())
                    .setString(GSI1SK, USER_PREFIX + user.getUsername().toLowerCase())
                    .setString(GSI2PK, USER_PREFIX + user.getEmail().toLowerCase())
                    .setString(GSI2SK, USER_PREFIX + user.getEmail().toLowerCase())
                    .setString(GSI3PK, USER_PREFIX + user.getCognitoUsername())
                    .setString(GSI3SK, USER_PREFIX + user.getCognitoUsername());
        }

        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(item.asMap())
                .build());
    }

    private AttributeValue mapFromEmailPreferences(EmailPreferences emailPreferences) {
        return new Item()
                .setBoolean("SendInviteEmail", emailPreferences.isSendInviteEmail())
                .set("TurnBasedPreferences", mapFromTurnBasedPreferences(emailPreferences.getTurnBasedPreferences()))
                .asAttributeValue();
    }

    private AttributeValue mapFromTurnBasedPreferences(TurnBasedPreferences turnBasedPreferences) {
        return new Item()
                .setBoolean("SendTurnEmail", turnBasedPreferences.isSendTurnEmail())
                .setBoolean("SendEndedEmail", turnBasedPreferences.isSendEndedEmail())
                .asAttributeValue();
    }

    @Override
    public void update(User user) throws ConcurrentModificationException {
        if (!user.isDeleted()) {
            findByEmail(user.getEmail())
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new EmailAlreadyInUse();
                    });
        }

        var updateItem = new UpdateItem()
                .setInt(VERSION, user.getVersion() + 1)
                .setString("Username", user.getUsername())
                .setString("CognitoUsername", user.getCognitoUsername())
                .setString("Email", user.getEmail())
                .setString("Language", user.getLanguage())
                .setString("Location", user.getLocation().orElse(null))
                .setString("TimeZone", user.getTimeZone().getId())
                .setInstant("Updated", user.getUpdated())
                .set("EmailPreferences", mapFromEmailPreferences(user.getEmailPreferences()))
                .setBoolean("Deleted", user.isDeleted());

        updateItem.expressionAttributeValue(":ExpectedVersion", Item.n(user.getVersion()));

        if (!user.isDeleted()) {
            updateItem.setString(GSI1PK, USER_PREFIX + user.getUsername().substring(0, 3).toLowerCase())
                    .setString(GSI1SK, USER_PREFIX + user.getUsername().toLowerCase())
                    .setString(GSI2PK, USER_PREFIX + user.getEmail().toLowerCase())
                    .setString(GSI2SK, USER_PREFIX + user.getEmail().toLowerCase())
                    .setString(GSI3PK, USER_PREFIX + user.getCognitoUsername())
                    .setString(GSI3SK, USER_PREFIX + user.getCognitoUsername());
        } else {
            updateItem.remove(GSI1PK, GSI1SK, GSI2PK, GSI2SK, GSI3PK, GSI3SK);
        }

        try {
            client.updateItem(UpdateItemRequest.builder()
                    .tableName(config.getTableName())
                    .key(Map.of(
                            PK, Item.s(USER_PREFIX + user.getId().getId()),
                            SK, Item.s(USER_PREFIX + user.getId().getId())
                    ))
                    .conditionExpression(VERSION + "=:ExpectedVersion")
                    .updateExpression(updateItem.getUpdateExpression())
                    .expressionAttributeNames(updateItem.getExpressionAttributeNames())
                    .expressionAttributeValues(updateItem.getExpressionAttributeValues())
                    .build());
        } catch (ConditionalCheckFailedException e) {
            throw new ConcurrentModificationException(e);
        }
    }

    @Override
    public void validateBeforeAdd(String email) {
        if (findByEmail(email).isPresent()) {
            throw new Users.EmailAlreadyInUse();
        }
    }
}
