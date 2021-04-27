package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.WebSocketConnectionDynamoDbRepository;
import com.boardgamefiesta.dynamodb.WebSocketConnectionDynamoDbRepositoryV2;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("triggerWebSocketConnectionV1ToV2")
public class TriggerWebSocketConnectionV1ToV2 extends DynamoDbTrigger {

    private final WebSocketConnectionDynamoDbRepository webSocketConnectionDynamoDbRepository;
    private final WebSocketConnectionDynamoDbRepositoryV2 webSocketConnectionDynamoDbRepositoryV2;

    @Inject
    public TriggerWebSocketConnectionV1ToV2(@NonNull DynamoDbClient client,
                                            @NonNull DynamoDbConfiguration config) {
        this.webSocketConnectionDynamoDbRepository = new WebSocketConnectionDynamoDbRepository(client, config);
        this.webSocketConnectionDynamoDbRepositoryV2 = new WebSocketConnectionDynamoDbRepositoryV2(client, config);
    }

    @Override
    void handleInsert(Map<String, AttributeValue> item) {
        var webSocketConnection = webSocketConnectionDynamoDbRepository.mapFromItem(item);
        webSocketConnectionDynamoDbRepositoryV2.add(webSocketConnection);
    }

    @Override
    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    @Override
    void handleRemove(Map<String, AttributeValue> key) {
        webSocketConnectionDynamoDbRepositoryV2.remove(key.get("Id").s());
    }
}
