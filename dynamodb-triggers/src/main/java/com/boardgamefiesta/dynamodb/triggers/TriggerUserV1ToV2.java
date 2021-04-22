package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepository;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepositoryV2;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("triggerUserV1ToV2")
@Slf4j
public class TriggerUserV1ToV2 extends DynamoDbTrigger {

    private final UserDynamoDbRepository userDynamoDbRepository;
    private final UserDynamoDbRepositoryV2 userDynamoDbRepositoryV2;

    @Inject
    public TriggerUserV1ToV2(@NonNull DynamoDbClient client,
                             @NonNull DynamoDbConfiguration config) {
        this.userDynamoDbRepository = new UserDynamoDbRepository(client, config);
        this.userDynamoDbRepositoryV2 = new UserDynamoDbRepositoryV2(client, config);
    }

    @Override
    void handleInsert(Map<String, AttributeValue> item) {
        var user = userDynamoDbRepository.mapToUser(item);
        userDynamoDbRepositoryV2.put(user);
    }

    @Override
    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    @Override
    void handleRemove(Map<String, AttributeValue> item) {
        // Not implemented
    }
}
