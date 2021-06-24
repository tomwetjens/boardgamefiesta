package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.featuretoggle.FeatureToggle;
import com.boardgamefiesta.domain.featuretoggle.FeatureToggles;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PK=FeatureToggle#<ID>
 * SK=FeatureToggle#<ID>
 */
@ApplicationScoped
public class FeatureToggleDynamoDbRepository implements FeatureToggles {

    private static final String PK = "PK";
    private static final String SK = "SK";

    private static final String FEATURE_TOGGLE_PREFIX = "FeatureToggle#";

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public FeatureToggleDynamoDbRepository(@NonNull DynamoDbClient client,
                                           @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Optional<FeatureToggle> findById(FeatureToggle.Id id) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + "=:SK")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(FEATURE_TOGGLE_PREFIX + id.name()),
                        ":SK", Item.s(FEATURE_TOGGLE_PREFIX + id.name())
                ))
                .build());

        if (response.hasItems() && !response.items().isEmpty()) {
            var item = Item.of(response.items().get(0));

            return Optional.of(FeatureToggle.builder()
                    .id(FeatureToggle.Id.valueOf(item.getString(PK).replace(FEATURE_TOGGLE_PREFIX, "")))
                    .userIds(item.getStrings("UserIds").stream()
                            .map(User.Id::of)
                            .collect(Collectors.toSet()))
                    .build());
        }
        return Optional.empty();
    }

}
