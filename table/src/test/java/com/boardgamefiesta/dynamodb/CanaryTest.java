/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
public class CanaryTest {

    @Mock
    DynamoDbConfiguration config;

    Games games = Games.all();

    UserDynamoDbRepositoryV2 users;

    TableDynamoDbRepositoryV2 tables;

    @BeforeEach
    void setUp() {
        when(config.tableName()).thenReturn("boardgamefiesta-prod");
        when(config.readGameIdShards()).thenReturn(2);
        when(config.writeGameIdShards()).thenReturn(2);

        var dynamoDbClient = DynamoDbClient.create();
        tables = new TableDynamoDbRepositoryV2(games, dynamoDbClient, config);
        users = new UserDynamoDbRepositoryV2(dynamoDbClient, config);
    }

    @Test
    void allTablesCanBeOpened() {
        var count = tables.findEndedWithHumanPlayers(Game.Id.of("gwt"), Integer.MAX_VALUE, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, true)
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

    @Disabled
    @Test
    void deleteTablesForUnfinishedGames() {
        var gameIdsToDelete = List.of(Game.Id.fromString("ds"), Game.Id.fromString("power-grid"));

        gameIdsToDelete.stream()
                .peek(gameId -> System.out.println("Finding tables for " + gameId.getId()))
                .flatMap(tables::findAllIds)
                .peek(tableId -> System.out.println("Deleting table " + tableId.getId()))
                .forEach(tables::delete);
    }

}
