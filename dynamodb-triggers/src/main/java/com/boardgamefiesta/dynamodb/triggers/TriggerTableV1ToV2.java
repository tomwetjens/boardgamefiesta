package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepository;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepositoryV2;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("triggerTableV1ToV2")
@Slf4j
public class TriggerTableV1ToV2 extends DynamoDbTrigger {

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
    void handleInsert(Map<String, AttributeValue> item) {
        if (isTable(item)) { // Ignore adjacency list items
            var table = tableDynamoDbRepository.mapToTable(item);
            tableDynamoDbRepositoryV2.put(table);
        }
    }

    @Override
    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    @Override
    void handleRemove(Map<String, AttributeValue> item) {
        if (isTable(item)) { // Ignore adjacency list items
            tableDynamoDbRepositoryV2.delete(Table.Id.of(item.get("Id").s()));
        }
    }

    private static boolean isTable(Map<String, AttributeValue> item) {
        return item.get("UserId").s().startsWith("Table-");
    }
}
