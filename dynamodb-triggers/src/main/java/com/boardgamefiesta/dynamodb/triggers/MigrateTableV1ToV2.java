package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MigrateTableV1ToV2 {

    public static void main(String[] args) {
        var sourceTableName = args[1];
        var targetTableName = args[2];
        var exclusiveStartId = args.length > 3 ?
                Map.of("Id", AttributeValue.builder().s(args[3]).build(),
                        "UserId", AttributeValue.builder().s(args[3]).build()) : null;

        var config = new DynamoDbConfiguration();
        config.setTableName(targetTableName);

        var client = DynamoDbClient.create();

        var triggerTableV1ToV2 = new TriggerTableV1ToV2(new Games(), client, config);

        System.out.println("Table: " + sourceTableName);
        var itemCount = client.describeTable(DescribeTableRequest.builder()
                .tableName(sourceTableName)
                .build())
                .table().itemCount();
        System.out.println("Estimated item count: " + itemCount);

        System.out.println("Scan: " + sourceTableName);
        var count = new AtomicLong(0);
        client.scanPaginator(ScanRequest.builder()
                .tableName(sourceTableName)
                .filterExpression("begins_with(UserId,:UserId)")
                .expressionAttributeValues(Map.of(":UserId", AttributeValue.builder().s("Table-").build()))
                .exclusiveStartKey(exclusiveStartId)
                .build())
                .items().stream()
                .forEach(item -> {
                    var n = count.incrementAndGet();
                    System.out.println(n + "/" + itemCount + ": " + sourceTableName + " " + targetTableName + " " + item.get("Id").s());
                    triggerTableV1ToV2.handleInsert(item);
                });
    }

}
