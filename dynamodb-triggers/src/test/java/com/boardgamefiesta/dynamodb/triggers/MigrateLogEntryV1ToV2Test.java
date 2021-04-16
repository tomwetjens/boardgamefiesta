package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

@Disabled
class MigrateLogEntryV1ToV2Test {

    DynamoDbConfiguration config = new DynamoDbConfiguration();
    DynamoDbClient client;

    MigrateLogEntryV1ToV2 migrateLogEntryV1ToV2;

    @BeforeEach
    void setUp() {
        config.setTableName("boardgamefiesta-dev");

        client = DynamoDbClient.create();

        migrateLogEntryV1ToV2 = new MigrateLogEntryV1ToV2(new Games(), client, config);
    }

    @Test
    void migrate() {
        client.scanPaginator(ScanRequest.builder()
                .tableName("gwt-log" + config.getTableSuffix().orElse(""))
                .build())
                .items().stream()
                .limit(10)
                .forEach(item -> migrateLogEntryV1ToV2.handleInsert(item));
    }
}