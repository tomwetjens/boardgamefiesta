package com.boardgamefiesta.dynamodb.triggers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepository;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepositoryV2;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("triggerLogEntryV1ToV2")
public class TriggerLogEntryV1ToV2 implements RequestHandler<DynamodbEvent, Void> {

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;
    private final TableDynamoDbRepository tableDynamoDbRepository;
    private final TableDynamoDbRepositoryV2 tableDynamoDbRepositoryV2;

    @Inject
    public TriggerLogEntryV1ToV2(@NonNull Games games,
                                 @NonNull DynamoDbClient client,
                                 @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
        this.tableDynamoDbRepository = new TableDynamoDbRepository(games, client, config);
        this.tableDynamoDbRepositoryV2 = new TableDynamoDbRepositoryV2(games, client, config);
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
        var logEntry = tableDynamoDbRepository.mapToLogEntry(item);
        var tableId = Table.Id.of(item.get("GameId").s());
        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(tableDynamoDbRepositoryV2.mapItemFromLogEntry(logEntry, tableId).asMap())
                .build());
    }

    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    void handleRemove(Map<String, AttributeValue> item) {
        // Not implemented
    }
}
