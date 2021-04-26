package com.boardgamefiesta.dynamodb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.dynamodb.DynaliteContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.lenient;

@Testcontainers
abstract class BaseDynamoDbRepositoryTest {

    static final String TABLE_NAME = "boardgamefiesta-unittest";

    @Container
    static DynaliteContainer dynaliteContainer = new DynaliteContainer("quay.io/testcontainers/dynalite:v1.2.1-1");

    static DynamoDbClient dynamoDbClient;
    static DynamoDbConfiguration config = new DynamoDbConfiguration();

    @BeforeAll
    static void beforeAll() {
        config.setTableName(TABLE_NAME);
        config.setReadGameIdShards(2);
        config.setWriteGameIdShards(2);

        dynamoDbClient = DynamoDbClient.create();
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://" + dynaliteContainer.getHost() + ":" + dynaliteContainer.getMappedPort(4567)))
                .build();

        createTable();
    }

    @BeforeEach
    void setUp() {
        deleteAll();
    }

    private static void deleteAll() {
        Chunked.stream(dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build())
                .items().stream()
                .map(item -> Map.of("PK", item.get("PK"), "SK", item.get("SK")))
                .map(key -> WriteRequest.builder()
                        .deleteRequest(DeleteRequest.builder()
                                .key(key)
                                .build())
                        .build()), 25)
                .map(writeRequests -> BatchWriteItemRequest.builder()
                        .requestItems(Map.of(TABLE_NAME, writeRequests.collect(Collectors.toList())))
                        .build())
                .forEach(dynamoDbClient::batchWriteItem);
    }

    private static void createTable() {
        // TODO Perform this from a CloudFormation template
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(
                        KeySchemaElement.builder()
                                .keyType(KeyType.HASH)
                                .attributeName("PK")
                                .build(),
                        KeySchemaElement.builder()
                                .keyType(KeyType.RANGE)
                                .attributeName("SK")
                                .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("PK")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("SK")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("GSI1PK")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("GSI1SK")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("GSI2PK")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("GSI2SK")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("GSI1")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .keyType(KeyType.HASH)
                                                .attributeName("GSI1PK")
                                                .build(),
                                        KeySchemaElement.builder()
                                                .keyType(KeyType.RANGE)
                                                .attributeName("GSI1SK")
                                                .build())
                                .projection(Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(5L)
                                        .writeCapacityUnits(5L)
                                        .build())
                                .build(),
                        GlobalSecondaryIndex.builder()
                                .indexName("GSI2")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .keyType(KeyType.HASH)
                                                .attributeName("GSI2PK")
                                                .build(),
                                        KeySchemaElement.builder()
                                                .keyType(KeyType.RANGE)
                                                .attributeName("GSI2SK")
                                                .build())
                                .projection(Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(5L)
                                        .writeCapacityUnits(5L)
                                        .build())
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                .build());

        BaseDynamoDbRepositoryTest.waitUntilTableCreated(dynamoDbClient);
    }

    private static void waitUntilTableCreated(DynamoDbClient dynamoDbClient) {
        try {
            while (dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .build()).table().tableStatus() == TableStatus.CREATING) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

}
