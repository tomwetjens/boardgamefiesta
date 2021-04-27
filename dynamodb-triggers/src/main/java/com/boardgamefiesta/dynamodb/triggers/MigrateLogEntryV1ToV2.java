package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.Item;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Map;

public class MigrateLogEntryV1ToV2 {

    public static void main(String[] args) {
        var sourceTableName = args[1];
        var targetTableName = args[2];
        var exclusiveStartId = args.length > 3 ?
                Map.of("GameId", Item.s(args[3]),
                        "Timestamp", AttributeValue.builder().n(args[4]).build()) : null;

        var config = new DynamoDbConfiguration();
        config.setTableName(targetTableName);

        var client = DynamoDbClient.create();

        var triggerLogEntryV1ToV2 = new TriggerLogEntryV1ToV2(new Games(), client, config);

        System.out.println("Scan: " + sourceTableName);
        client.scanPaginator(ScanRequest.builder()
                .tableName(sourceTableName)
                .exclusiveStartKey(exclusiveStartId)
                .build())
                .items().stream()
                .forEach(item -> {
                    System.out.println("GameId: " + item.get("GameId").s() + " Timestamp: " + item.get("Timestamp").n());
                    triggerLogEntryV1ToV2.handleInsert(item);
                });
    }

}
