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

package com.boardgamefiesta.server.query;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.rating.RatingAdjuster;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.dynamodb.DynamoDbConfiguration;
import com.boardgamefiesta.dynamodb.RatingDynamoDbRepositoryV2;
import com.boardgamefiesta.dynamodb.TableDynamoDbRepositoryV2;
import com.boardgamefiesta.dynamodb.UserDynamoDbRepositoryV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
@Disabled
class RecalculateAllRatingsTest {

    @Mock
    DynamoDbConfiguration config;

    Users users;
    Ratings ratings;
    Tables tables;

    RatingAdjuster ratingAdjuster;

    @BeforeEach
    void setUp() {
        var dynamoDbClient = DynamoDbClient.create();
        users = new UserDynamoDbRepositoryV2(dynamoDbClient, config);
        ratings = new RatingDynamoDbRepositoryV2(dynamoDbClient, config);
        tables = new TableDynamoDbRepositoryV2(new Games(), dynamoDbClient, config);

        ratingAdjuster = new RatingAdjuster(tables, ratings);
    }

    @Test
    void run() {
        tables.findEndedWithHumanPlayers(Game.Id.of("gwt"), Integer.MAX_VALUE, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, true)
                .forEach(this::recalculate);
    }

    @Test
    void singleTable() {
        tables.findById(Table.Id.of("bad22dd0-ae66-4041-b138-201c28bc6fd8"))
                .ifPresent(this::recalculate);

        //
        // eldzik 743cd118-b031-430c-a7e4-ef4ea8193122
        // friar_ken db3bb947-c0de-498f-aae3-d4ba1ce037b7
        // elmermad 10a80fdc-4e56-4ea4-8c8e-4cdae137e2e1
        // moritz 07205dca-2c60-47d5-b818-06629c7c96a1

        // Table.Id(id=bad22dd0-ae66-4041-b138-201c28bc6fd8) 2021-03-26T18:30:18Z
        // eldzik -> 1065
        // friar_ken -> 1243
        // moritz -> 1229
        // elmermad -> 1052
    }

    private void recalculate(Table table) {
        var oldRatings = table.getPlayers().stream()
                .map(Player::getUserId)
                .flatMap(Optional::stream)
                .flatMap(userId -> ratings.findByTable(userId, table.getId()).stream())
                .sorted(Comparator.comparing(rating -> rating.getUserId().getId()))
                .collect(Collectors.toList());

        System.out.println(table.getId() + " " + table.getEnded());

        if (!table.hasComputerPlayers()) {
            var newRatings = ratingAdjuster.adjustRatings(table).stream()
                    .sorted(Comparator.comparing(rating -> rating.getUserId().getId()))
                    .collect(Collectors.toList());

            newRatings.forEach(rating -> {
                System.out.println(rating.getUserId().getId() + " -> " + rating.getRating());
            });

            ratings.addAll(newRatings);

            var oldRatingsToDelete = oldRatings.stream()
                    .filter(oldRating -> newRatings.stream()
                            .noneMatch(newRating -> newRating.getTimestamp().toEpochMilli()
                                    == oldRating.getTimestamp().toEpochMilli()))
                    .collect(Collectors.toList());
            // TODO Implement delete
//            oldRatingsToDelete.forEach(ratings::delete);
        } else {
            // TODO Implement delete
//            oldRatings.forEach(ratings::delete);
        }
    }
}