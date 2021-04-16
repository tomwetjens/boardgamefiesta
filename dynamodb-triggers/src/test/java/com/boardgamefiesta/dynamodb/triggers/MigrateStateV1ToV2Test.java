package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

@Disabled
class MigrateStateV1ToV2Test {

    DynamoDbConfiguration config = new DynamoDbConfiguration();
    DynamoDbClient client;

    MigrateStateV1ToV2 migrateStateV1ToV2;

    @BeforeEach
    void setUp() {
        config.setTableName("boardgamefiesta-dev");

        client = DynamoDbClient.create();

        migrateStateV1ToV2 = new MigrateStateV1ToV2(client, config);
    }

    @Test
    void migrate() {
        client.scanPaginator(ScanRequest.builder()
                .tableName("gwt-state" + config.getTableSuffix().orElse(""))
                .build())
                .items().stream()
                .limit(10)
                .forEach(item -> migrateStateV1ToV2.handleInsert(item));
    }
}