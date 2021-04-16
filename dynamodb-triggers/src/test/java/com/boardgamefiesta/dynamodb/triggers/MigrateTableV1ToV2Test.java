package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Map;

@Disabled
class MigrateTableV1ToV2Test {

    DynamoDbConfiguration config = new DynamoDbConfiguration();
    DynamoDbClient client;

    MigrateTableV1ToV2 migrateTableV1ToV2;

    @BeforeEach
    void setUp() {
        config.setTableName("boardgamefiesta-dev");

        client = DynamoDbClient.create();

        migrateTableV1ToV2 = new MigrateTableV1ToV2(new Games(), client, config);
    }

    @Test
    void migrate() {
        client.scanPaginator(ScanRequest.builder()
                .tableName("gwt-games" + config.getTableSuffix().orElse(""))
                .filterExpression("begins_with(UserId,:Prefix)")
                .expressionAttributeValues(Map.of(
                        ":Prefix", AttributeValue.builder().s("Table-").build()
                ))
                .build())
                .items().stream()
                .limit(10)
                .forEach(item -> migrateTableV1ToV2.handleInsert(item));
    }
}