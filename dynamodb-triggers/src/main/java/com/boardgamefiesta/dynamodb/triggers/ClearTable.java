package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.Chunked;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClearTable {

    public static void main(String[] args) {
        var tableName = args[1];

        var config = new DynamoDbConfiguration();
        config.setTableName(tableName);

        var client = DynamoDbClient.create();

        System.out.println("Table: " + tableName);
        var keySchema = client.describeTable(DescribeTableRequest.builder()
                .tableName(tableName)
                .build())
                .table().keySchema();
        System.out.println("Key schema: " + keySchema);

        int delaySeconds = 10;
        while (delaySeconds > 0) {
            System.out.println("Clearing table: " + tableName + " in " + delaySeconds + " sec!!!!");
            delaySeconds--;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Clearing table: " + tableName + "...");

        deleteAll(client, tableName, keySchema);

    }

    private static void deleteAll(DynamoDbClient client, String tableName, List<KeySchemaElement> keySchema) {
        Chunked.stream(client.scanPaginator(ScanRequest.builder()
                .tableName(tableName)
                .build())
                .items().stream()
                .map(item -> keySchema.stream()
                        .collect(Collectors.toMap(KeySchemaElement::attributeName, e -> item.get(e.attributeName()))))
                .map(key -> WriteRequest.builder()
                        .deleteRequest(DeleteRequest.builder()
                                .key(key)
                                .build())
                        .build()), 25)
                .map(writeRequests -> BatchWriteItemRequest.builder()
                        .requestItems(Map.of(tableName, writeRequests.collect(Collectors.toList())))
                        .build())
                .forEach(client::batchWriteItem);
    }

}
