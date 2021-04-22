package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

class TriggerRatingV1ToV2Test {

    DynamoDbConfiguration config = new DynamoDbConfiguration();
    DynamoDbClient client;

    TriggerRatingV1ToV2 triggerRatingV1ToV2;

    @BeforeEach
    void setUp() {
        config.setTableName("boardgamefiesta-dev");

        client = DynamoDbClient.create();

        triggerRatingV1ToV2 = new TriggerRatingV1ToV2(client, config);
    }

    @Test
    @Disabled
    void deleteAllItems() {
        client.scanPaginator(ScanRequest.builder()
                .tableName(config.getTableName())
                .projectionExpression("PK,SK")
                .build())
                .items()
                .forEach(item -> client.deleteItem(DeleteItemRequest.builder()
                        .tableName(config.getTableName())
                        .key(item)
                        .build()));
    }

    @Test
    void migrate() {
        client.scanPaginator(ScanRequest.builder()
                .tableName("gwt-ratings" + config.getTableSuffix().orElse(""))
                .build())
                .items().stream()
                .limit(10)
                .forEach(item -> triggerRatingV1ToV2.handleInsert(item));
    }
}