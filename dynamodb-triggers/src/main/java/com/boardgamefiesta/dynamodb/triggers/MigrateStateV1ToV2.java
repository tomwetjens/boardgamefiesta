package com.boardgamefiesta.dynamodb.triggers;

import com.boardgamefiesta.dynamodb.Chunked;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MigrateStateV1ToV2 {

    public static void main(String[] args) {
        var sourceTableName = args[1];
        var targetTableName = args[2];
        var exclusiveStartId = args.length > 3 ?
                Map.of("TableId", AttributeValue.builder().s(args[3]).build(),
                        "Timestamp", AttributeValue.builder().n(args[4]).build()) : null;

        var config = new DynamoDbConfiguration();
        config.setTableName(targetTableName);

        var client = DynamoDbClient.create();

        var triggerStateV1ToV2 = new TriggerStateV1ToV2(client, config);

        System.out.println("Table: " + sourceTableName);
        var itemCount = client.describeTable(DescribeTableRequest.builder()
                .tableName(sourceTableName)
                .build())
                .table().itemCount();
        System.out.println("Estimated item count: " + itemCount);

        var threads = 10;
        System.out.println("Scan: " + sourceTableName);
        var count = new AtomicLong(0);
        IntStream.range(0, threads)
                .parallel()
                .forEach(segment ->
                        Chunked.stream(client.scanPaginator(ScanRequest.builder()
                                .tableName(sourceTableName)
                                .exclusiveStartKey(exclusiveStartId)
                                .segment(segment)
                                .totalSegments(threads)
                                .build())
                                .stream()
                                .peek(response -> System.out.println("response returned " + response.count() + " items"))
                                .filter(ScanResponse::hasItems)
                                .flatMap(response -> response.items().stream()), 25)
                                .forEach(items -> client.batchWriteItem(BatchWriteItemRequest.builder()
                                        .requestItems(Map.of(targetTableName, items
                                                .peek(item -> {
                                                    var n = count.incrementAndGet();
                                                    System.out.println(n + "/" + itemCount + ": " + sourceTableName + " " + targetTableName + " " + item.get("TableId").s() + " " + item.get("Timestamp").n());
                                                })
                                                .map(item -> WriteRequest.builder()
                                                        .putRequest(PutRequest.builder()
                                                                .item(triggerStateV1ToV2.migrate(item))
                                                                .build())
                                                        .build())
                                                .collect(Collectors.toList())))
                                        .build())));
    }

}
