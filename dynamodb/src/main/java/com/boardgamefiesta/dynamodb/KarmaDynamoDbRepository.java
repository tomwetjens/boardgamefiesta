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

import com.boardgamefiesta.domain.karma.Karma;
import com.boardgamefiesta.domain.karma.Karmas;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;

/**
 * Entity type: Karma
 * PK = User#id
 * SK = Karma#timestamp
 */
@ApplicationScoped
@Slf4j
public class KarmaDynamoDbRepository implements Karmas {

    private static final String PK = "PK";
    private static final String SK = "SK";

    private static final String USER_PREFIX = "User#";
    private static final String KARMA_PREFIX = "Karma#";

    private static final DateTimeFormatter TIMESTAMP_SECS_FORMATTER = new DateTimeFormatterBuilder()
            .parseStrict()
            .appendInstant(0) // No fractional second
            .toFormatter();

    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public KarmaDynamoDbRepository(@NonNull DynamoDbClient client,
                                   @NonNull DynamoDbConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Karma current(@NonNull User.Id userId, @NonNull Instant at) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .keyConditionExpression(PK + "=:PK AND " + SK + " BETWEEN :SKFrom AND :SKTo")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(USER_PREFIX + userId.getId()),
                        ":SKFrom", Item.s(KARMA_PREFIX + TIMESTAMP_SECS_FORMATTER.format(MIN_TIMESTAMP)),
                        ":SKTo", Item.s(KARMA_PREFIX + TIMESTAMP_SECS_FORMATTER.format(at))
                ))
                .scanIndexForward(false)
                .limit(1)
                .build())
                .items().stream()
                .findFirst()
                .map(Item::of)
                .map(this::mapToKarma)
                .orElse(Karma.initial(userId));
    }

    @Override
    public void add(Karma karma) {
        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(mapFromKarma(karma).asMap())
                .build());
    }

    private Item mapFromKarma(Karma karma) {
        return new Item()
                .setString(PK, USER_PREFIX + karma.getUserId().getId())
                .setString(SK, KARMA_PREFIX + TIMESTAMP_SECS_FORMATTER.format(karma.getTimestamp()))
                .setString("UserId", karma.getUserId().getId())
                .setInstant("Timestamp", karma.getTimestamp())
                .setInt("Karma", karma.getKarma())
                .setInt("Delta", karma.getDelta())
                .setEnum("Event", karma.getEvent())
                .setString("TableId", karma.getTableId().map(Table.Id::getId).orElse(null));
    }

    private Karma mapToKarma(Item item) {
        return Karma.builder()
                .userId(User.Id.of(item.getString("UserId")))
                .timestamp(item.getInstant("Timestamp"))
                .karma(item.getInt("Karma"))
                .delta(item.getOptionalInt("Delta").orElse(0))
                .event(item.getOptionalEnum("Event", Karma.Event.class).orElse(Karma.Event.UNKNOWN))
                .tableId(item.getOptionalString("TableId").map(Table.Id::of).orElse(null))
                .build();
    }
}
