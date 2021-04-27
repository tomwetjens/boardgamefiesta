package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Map;

public class MigrateUserV1ToV2 {

    public static void main(String[] args) {
        var sourceTableName = args[1];
        var targetTableName = args[2];
        var exclusiveStartId = args.length > 3 ?
                AttributeValue.builder().s(args[3]).build() : null;

        var config = new DynamoDbConfiguration();
        config.setTableName(targetTableName);

        var client = DynamoDbClient.create();

        var triggerUserV1ToV2 = new TriggerUserV1ToV2(client, config);

        System.out.println("Scan: " + sourceTableName);
        client.scanPaginator(ScanRequest.builder()
                .tableName(sourceTableName)
                .exclusiveStartKey(exclusiveStartId != null ? Map.of("Id", exclusiveStartId) : null)
                .build())
                .items().stream()
                .forEach(item -> {
                    System.out.println("Id: " + item.get("Id").s());
                    triggerUserV1ToV2.handleInsert(item);
                });
    }

}
