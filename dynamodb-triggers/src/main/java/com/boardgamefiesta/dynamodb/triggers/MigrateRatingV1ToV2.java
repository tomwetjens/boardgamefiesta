package com.boardgamefiesta.dynamodb.triggers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.boardgamefiesta.domain.rating.Ranking;
import com.boardgamefiesta.dynamodb.*;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

@Named("migrateRatingV1ToV2")
public class MigrateRatingV1ToV2 implements RequestHandler<DynamodbEvent, Void> {

    private final RatingDynamoDbRepository ratingDynamoDbRepository;
    private final RatingDynamoDbRepositoryV2 ratingDynamoDbRepositoryV2;

    @Inject
    public MigrateRatingV1ToV2(@NonNull DynamoDbClient client,
                               @NonNull DynamoDbConfiguration config) {
        this.ratingDynamoDbRepository = new RatingDynamoDbRepository(client, config);
        this.ratingDynamoDbRepositoryV2 = new RatingDynamoDbRepositoryV2(client, config);
    }

    @Override
    public Void handleRequest(DynamodbEvent event, Context context) {
        event.getRecords().forEach(record -> {
            switch (OperationType.fromValue(record.getEventName())) {
                case INSERT:
                    handleInsert(AttributeValues.toClientModel(record.getDynamodb().getNewImage()));
                    break;
                case MODIFY:
                    handleModify(AttributeValues.toClientModel(record.getDynamodb().getNewImage()));
                    break;
                case REMOVE:
                    handleRemove(AttributeValues.toClientModel(record.getDynamodb().getOldImage()));
                    break;
            }
        });
        return null;
    }

    void handleInsert(Map<String, AttributeValue> item) {
        var rating = ratingDynamoDbRepository.mapToRating(item);
        ratingDynamoDbRepositoryV2.addAll(Collections.singleton(rating));
    }

    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    void handleRemove(Map<String, AttributeValue> item) {
        // Not implemented
    }
}
