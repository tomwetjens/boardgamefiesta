package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.gwt.GWTProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@Disabled
public class CanaryTest {

    @Mock
    DynamoDbConfiguration config;

    Games games = new Games();

    Tables tables;

    @Test
    void allTablesCanBeOpened() {
        tables = new TableDynamoDbRepositoryV2(games, DynamoDbClient.create(), config);

        var count = tables.findEnded(Game.Id.of(GWTProvider.ID), Integer.MAX_VALUE, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, true)
                .filter(table -> table.getStatus() == Table.Status.STARTED || table.getStatus() == Table.Status.ENDED)
                .peek(table -> {
                    System.out.println(table.getId().getId());
                    try {
                        table.getState();
                    } catch (Exception e) {
                        throw new AssertionError("Failed: " + table.getId(), e);
                    }
                })
                .count();
        assertThat(count).isGreaterThan(0);

        System.out.println("Checked " + count + " tables");
    }

}
