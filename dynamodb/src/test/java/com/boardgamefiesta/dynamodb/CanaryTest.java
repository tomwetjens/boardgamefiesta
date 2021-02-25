package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
public class CanaryTest {

    @Mock
    DynamoDbConfiguration config;

    Games games = new Games();

    TableDynamoDbRepository tableDynamodbRepository;

    @Test
    void allTablesCanBeOpened() {
        when(config.getTableSuffix()).thenReturn(Optional.of(""));

        tableDynamodbRepository = new TableDynamoDbRepository(games, DynamoDbClient.create(), config);

        var count = tableDynamodbRepository.findAll()
                .filter(table -> table.getStatus() == Table.Status.STARTED || table.getStatus() == Table.Status.ENDED)
                .peek(table -> {
                    try {
                        table.getCurrentPlayers();
                    } catch (Exception e) {
                        throw new AssertionError("Failed: " + table.getId(), e);
                    }
                })
                .count();
        assertThat(count).isGreaterThan(0);

        System.out.println("Checked " + count + " tables");
    }
}
