package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

class MigrateUserV1ToV2Test {

    DynamoDbConfiguration config = new DynamoDbConfiguration();
    DynamoDbClient client;

    MigrateUserV1ToV2 migrateUserV1ToV2;

    @BeforeEach
    void setUp() {
        config.setTableName("boardgamefiesta-dev");

        client = DynamoDbClient.create();

        migrateUserV1ToV2 = new MigrateUserV1ToV2(client, config);
    }

    @Test
    void migrate() {
        client.scanPaginator(ScanRequest.builder()
                .tableName("gwt-users" + config.getTableSuffix().orElse(""))
                .build())
                .items().stream()
                .limit(10)
                .forEach(item -> migrateUserV1ToV2.handleInsert(item));
    }
}