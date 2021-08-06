/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
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

        var count = tables.findEndedWithHumanPlayers(Game.Id.of(GWTProvider.ID), Integer.MAX_VALUE, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, true)
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
