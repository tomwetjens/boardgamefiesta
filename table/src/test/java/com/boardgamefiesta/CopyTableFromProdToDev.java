package com.boardgamefiesta;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepositoryV2;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepositoryV2;
import com.boardgamefiesta.test.cdi.MockCDI;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.enterprise.inject.spi.CDI;
import java.time.Instant;

public class CopyTableFromProdToDev {

    public static void main(String[] args) {
        var id = Table.Id.fromString(args.length >=1 ? args[0] : "05ab88ba-02a3-4a8e-bcac-5250c0de1581");
        var timestamp = Instant.parse(args.length >= 2 ? args[1] : "?");
        var username = args.length >= 3 ? args[2] : "tom";

        CDI.setCDIProvider(MockCDI::new); // needed for domain events

        Games games = Games.all();

        var dynamoDbClient = DynamoDbClient.create();

        var prodTables = new TableDynamoDbRepositoryV2(games, dynamoDbClient, prodConfig());

        var devTables = new TableDynamoDbRepositoryV2(games, dynamoDbClient, devConfig());
        var devUsers = new UserDynamoDbRepositoryV2(dynamoDbClient, devConfig());

        var table = prodTables.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id.getId()));

        var owner = devUsers.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        var fork = table.fork(timestamp, Table.Type.REALTIME, Table.Mode.PRACTICE, owner);

        devTables.add(fork);

        System.out.println("Created table: " + fork.getId());
    }

    private static DynamoDbConfiguration prodConfig() {
        return new DynamoDbConfiguration() {
            @Override
            public String tableName() {
                return "boardgamefiesta-prod";
            }

            @Override
            public int writeGameIdShards() {
                return 2;
            }

            @Override
            public int readGameIdShards() {
                return 2;
            }
        };
    }

    private static DynamoDbConfiguration devConfig() {
        return new DynamoDbConfiguration() {
            @Override
            public String tableName() {
                return "boardgamefiesta-dev";
            }

            @Override
            public int writeGameIdShards() {
                return 2;
            }

            @Override
            public int readGameIdShards() {
                return 2;
            }
        };
    }

}
