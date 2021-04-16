package com.boardgamefiesta.dynamodb.triggers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.Item;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Named("migrateStateV1ToV2")
public class MigrateStateV1ToV2 implements RequestHandler<DynamodbEvent, Void> {

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public MigrateStateV1ToV2(@NonNull DynamoDbClient client,
                              @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
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
        var timestamp = Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n()));

        var newItem = new HashMap<String, AttributeValue>();
        newItem.put("PK", Item.s("Table#" + item.get("TableId").s()));
        newItem.put("SK", Item.s("State#" + timestamp.toString()));
        newItem.put("Timestamp", Item.s(timestamp));
        newItem.put("State", item.get("State"));

        Optional.ofNullable(item.get("Previous"))
                .filter(attributeValue -> !Boolean.TRUE.equals(attributeValue.nul()))
                .map(AttributeValue::n)
                .map(Long::parseLong)
                .map(Instant::ofEpochMilli)
                .ifPresent(previous -> newItem.put("Previous", Item.s(previous)));

        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(newItem)
                .build());
    }

    void handleModify(Map<String, AttributeValue> item) {
        handleInsert(item);
    }

    void handleRemove(Map<String, AttributeValue> item) {
        var timestamp = Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n()));

        client.deleteItem(DeleteItemRequest.builder()
                .tableName(config.getTableName())
                .key(Map.of(
                        "PK", Item.s("Table#" + item.get("TableId").s()),
                        "SK", Item.s("State#" + timestamp.toString())
                ))
                .build());
    }
}
