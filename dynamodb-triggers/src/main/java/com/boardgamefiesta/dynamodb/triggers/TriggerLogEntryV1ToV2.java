package com.boardgamefiesta.dynamodb.triggers;

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
public class TriggerLogEntryV1ToV2 extends DynamoDbTrigger{

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
    void handleInsert(Map<String, AttributeValue> item) {
        var logEntry = tableDynamoDbRepository.mapToLogEntry(item);
        var tableId = Table.Id.of(item.get("GameId").s());
        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(tableDynamoDbRepositoryV2.mapItemFromLogEntry(logEntry, tableId).asMap())
                .build());
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
