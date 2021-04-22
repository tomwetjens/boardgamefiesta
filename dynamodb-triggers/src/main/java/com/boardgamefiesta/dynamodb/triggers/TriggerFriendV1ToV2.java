package com.boardgamefiesta.dynamodb.triggers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.dynamodb.*;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("triggerFriendV1ToV2")
public class TriggerFriendV1ToV2 implements RequestHandler<DynamodbEvent, Void> {

    private final FriendDynamoDbRepository friendDynamoDbRepository;
    private final FriendDynamoDbRepositoryV2 friendDynamoDbRepositoryV2;

    @Inject
    public TriggerFriendV1ToV2(@NonNull DynamoDbClient client,
                               @NonNull DynamoDbConfiguration config) {
        this.friendDynamoDbRepository = new FriendDynamoDbRepository(client, config);
        this.friendDynamoDbRepositoryV2 = new FriendDynamoDbRepositoryV2(client, config);
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
        var friend = friendDynamoDbRepository.mapItemToFriend(item);
        friendDynamoDbRepositoryV2.add(friend);
    }

    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    void handleRemove(Map<String, AttributeValue> item) {
        var friend = friendDynamoDbRepository.mapItemToFriend(item);
        friendDynamoDbRepositoryV2.delete(friend.getId());
    }
}
