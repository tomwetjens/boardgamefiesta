package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.Item;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Map;

public class MigrateRatingV1ToV2 {

    public static void main(String[] args) {
        var sourceTableName = args[1];
        var targetTableName = args[2];
        var exclusiveStartId = args.length > 3 ?
                Map.of("UserIdGameId", Item.s(args[3]),
                        "Timestamp", AttributeValue.builder().n(args[4]).build()) : null;

        var config = new DynamoDbConfiguration();
        config.setTableName(targetTableName);

        var client = DynamoDbClient.create();

        var triggerRatingV1ToV2 = new TriggerRatingV1ToV2(client, config);

        System.out.println("Scan: " + sourceTableName);
        client.scanPaginator(ScanRequest.builder()
                .tableName(sourceTableName)
                .exclusiveStartKey(exclusiveStartId)
                .build())
                .items().stream()
                .forEach(item -> {
                    System.out.println("UserIdGameId: " + item.get("UserIdGameId").s() + " Timestamp: " + item.get("Timestamp").n());
                    triggerRatingV1ToV2.handleInsert(item);
                });
    }

}
