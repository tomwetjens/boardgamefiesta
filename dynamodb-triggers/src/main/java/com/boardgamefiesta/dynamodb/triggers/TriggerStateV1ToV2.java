package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepositoryV2;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Named("triggerStateV1ToV2")
public class TriggerStateV1ToV2 extends DynamoDbTrigger {

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public TriggerStateV1ToV2(@NonNull DynamoDbClient client,
                              @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    void handleInsert(Map<String, AttributeValue> item) {
        var tableId = Table.Id.of(item.get("TableId").s());
        var timestamp = Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n()));

        var previousTimestamp = Optional.ofNullable(item.get("Previous"))
                .filter(attributeValue -> !Boolean.TRUE.equals(attributeValue.nul()))
                .map(AttributeValue::n)
                .map(Long::parseLong)
                .map(Instant::ofEpochMilli);

        var state = item.get("State");

        var newItem = TableDynamoDbRepositoryV2.mapItemFromState(tableId, timestamp, previousTimestamp, state);

        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(newItem)
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
