package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.*;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("triggerFriendV1ToV2")
public class TriggerFriendV1ToV2 extends DynamoDbTrigger {

    private final FriendDynamoDbRepository friendDynamoDbRepository;
    private final FriendDynamoDbRepositoryV2 friendDynamoDbRepositoryV2;

    @Inject
    public TriggerFriendV1ToV2(@NonNull DynamoDbClient client,
                               @NonNull DynamoDbConfiguration config) {
        this.friendDynamoDbRepository = new FriendDynamoDbRepository(client, config);
        this.friendDynamoDbRepositoryV2 = new FriendDynamoDbRepositoryV2(client, config);
    }

    @Override
    void handleInsert(Map<String, AttributeValue> item) {
        var friend = friendDynamoDbRepository.mapItemToFriend(item);
        friendDynamoDbRepositoryV2.add(friend);
    }

    @Override
    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    @Override
    void handleRemove(Map<String, AttributeValue> key) {
        var friend = friendDynamoDbRepository.mapItemToFriend(key);
        friendDynamoDbRepositoryV2.delete(friend.getId());
    }
}
