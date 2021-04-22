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

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("triggerTableV1ToV2")
public class TriggerTableV1ToV2 implements RequestHandler<DynamodbEvent, Void> {

    private final TableDynamoDbRepository tableDynamoDbRepository;
    private final TableDynamoDbRepositoryV2 tableDynamoDbRepositoryV2;

    @Inject
    public TriggerTableV1ToV2(@NonNull Games games,
                              @NonNull DynamoDbClient client,
                              @NonNull DynamoDbConfiguration config) {
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
        if (item.get("UserId").s().startsWith("Table-")) { // Ignore adjacency list items
            var table = tableDynamoDbRepository.mapToTable(item);
            tableDynamoDbRepositoryV2.add(table);
        }
    }

    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    void handleRemove(Map<String, AttributeValue> item) {
        if (item.get("UserId").s().startsWith("Table-")) { // Ignore adjacency list items
            tableDynamoDbRepositoryV2.delete(Table.Id.of(item.get("Id").s()));
        }
    }
}
