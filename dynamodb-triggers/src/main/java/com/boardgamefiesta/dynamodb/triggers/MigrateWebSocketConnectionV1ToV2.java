package com.boardgamefiesta.dynamodb.triggers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.WebSocketConnectionDynamoDbRepository;
import com.boardgamefiesta.dynamodb.WebSocketConnectionDynamoDbRepositoryV2;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("migrateWebSocketConnectionV1ToV2")
public class MigrateWebSocketConnectionV1ToV2 implements RequestHandler<DynamodbEvent, Void> {

    private final WebSocketConnectionDynamoDbRepository webSocketConnectionDynamoDbRepository;
    private final WebSocketConnectionDynamoDbRepositoryV2 webSocketConnectionDynamoDbRepositoryV2;

    @Inject
    public MigrateWebSocketConnectionV1ToV2(@NonNull DynamoDbClient client,
                                            @NonNull DynamoDbConfiguration config) {
        this.webSocketConnectionDynamoDbRepository = new WebSocketConnectionDynamoDbRepository(client, config);
        this.webSocketConnectionDynamoDbRepositoryV2 = new WebSocketConnectionDynamoDbRepositoryV2(client, config);
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
        var webSocketConnection = webSocketConnectionDynamoDbRepository.mapFromItem(item);
        webSocketConnectionDynamoDbRepositoryV2.add(webSocketConnection);
    }

    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    void handleRemove(Map<String, AttributeValue> item) {
        webSocketConnectionDynamoDbRepositoryV2.remove(item.get("Id").s());
    }
}
