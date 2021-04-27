package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.Item;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Map;

public class MigrateWebSocketConnectionV1ToV2 {

    public static void main(String[] args) {
        var sourceTableName = args[1];
        var targetTableName = args[2];
        var exclusiveStartId = args.length > 3 ?
                Map.of("Id", Item.s(args[3])) : null;

        var config = new DynamoDbConfiguration();
        config.setTableName(targetTableName);

        var client = DynamoDbClient.create();

        var triggerWebSocketConnectionV1ToV2 = new TriggerWebSocketConnectionV1ToV2(client, config);

        System.out.println("Scan: " + sourceTableName);
        client.scanPaginator(ScanRequest.builder()
                .tableName(sourceTableName)
                .exclusiveStartKey(exclusiveStartId)
                .build())
                .items().stream()
                .forEach(item -> {
                    System.out.println("Id: " + item.get("Id").s());
                    triggerWebSocketConnectionV1ToV2.handleInsert(item);
                });
    }

}
