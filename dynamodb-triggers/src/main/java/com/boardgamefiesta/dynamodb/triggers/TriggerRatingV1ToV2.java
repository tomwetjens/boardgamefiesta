package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.*;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

@Named("triggerRatingV1ToV2")
public class TriggerRatingV1ToV2 extends DynamoDbTrigger {

    private final RatingDynamoDbRepository ratingDynamoDbRepository;
    private final RatingDynamoDbRepositoryV2 ratingDynamoDbRepositoryV2;

    @Inject
    public TriggerRatingV1ToV2(@NonNull DynamoDbClient client,
                               @NonNull DynamoDbConfiguration config) {
        this.ratingDynamoDbRepository = new RatingDynamoDbRepository(client, config);
        this.ratingDynamoDbRepositoryV2 = new RatingDynamoDbRepositoryV2(client, config);
    }

    @Override
    void handleInsert(Map<String, AttributeValue> item) {
        var rating = ratingDynamoDbRepository.mapToRating(item);
        ratingDynamoDbRepositoryV2.addAll(Collections.singleton(rating));
    }

    @Override
    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    @Override
    void handleRemove(Map<String, AttributeValue> key) {
        // Not implemented
    }
}
