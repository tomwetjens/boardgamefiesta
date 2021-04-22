package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.*;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.dynamodb.json.DynamoDbJson;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Item Collection (Single table design) per Table:
 * <p>
 * GSI1 index is always filled and is used to query all tables by game or user.
 * Examples:
 * GSI1PK=Game#ID#Shard
 * GSI1PK=User#ID
 * <p>
 * GSI1SK is chosen so that, when querying with ScanIndexForward=false, the order is:
 * by ended date, then by started date, then by created date
 * Examples:
 * GSI1SK=Table#2021-03-04T12:23:00#ID
 * GSI1SK=Table#2021-03-05T19:45:00#ID
 * <p>
 * GSI2 is a sparse index only filled if the table is active (i.e. not ended or abandoned) and is used to query only active tables by game or user.
 * GSI2SK is chosen so that, when querying with ScanIndexForward=false, the order is:
 * * joinable tables (i.e. open and seats available) by Created descending
 * * then not started non-joinable new tables sorted by Created descending
 * * then started tables sorted by Started descending
 * * then ended tables sorted by Ended descending
 * * Examples:
 * * GSI1SK=Table#2021-03-04T12:23:00#ID
 * * GSI1SK=Table#2021-03-05T19:45:00#ID
 * * GSI1SK=Table#ASTARTED#2021-03-05T18:20:00#ID
 * * GSI1SK=Table#STARTED#2021-03-05T19:45:00#ID
 * * GSI1SK=Table#NEW#2021-03-05T18:20:00#ID
 * * GSI1SK=Table#NEW#2021-03-05T19:45:00#ID
 * * GSI1SK=Table#OPEN#2021-03-05T18:20:00#ID
 * * GSI1SK=Table#OPEN#2021-03-05T19:45:00#ID
 * <p>
 * GSI1PK and GSI2PK with Game ID is write sharded, because there can be MANY tables per Game ID which will make GSI1 have hot partitions.
 * <p>
 * Log Entry:      PK=Table#ID SK=LogEntry#2021-03-05T19:45:00
 * Player:         PK=Table#ID SK=Player#ID GSI1PK=User#ID       GSI1SK=2021-03-05T19:45:00#Table#ID GSI2PK=User#ID GSI2SK=Table#2021-03-05T19:45:00#ID
 * Historic State: PK=Table#ID SK=State#2021-03-05T19:45:00
 * Current State:  PK=Table#ID SK=State#2021-03-05T19:51:00
 * Table:          PK=Table#ID SK=Table     GSI1PK=Game#ID#Shard GSI1SK=2021-03-05T19:45:00#Table#ID GSI2PK=Game#ID#Shard GSI2SK=Table#2021-03-05T19:45:00#ID
 * <p>
 * SK has been chosen so Table and Players can be retrieved in one shot.
 * <p>
 * When a table is abandoned:
 * - If not started, then it is Expired immediately so it will no longer be in views
 * - If already started, then it is Expired after some time
 * <p>
 * Find "my tables" (active)
 */
//@ApplicationScoped
@Slf4j
public class TableDynamoDbRepositoryV2 implements Tables {

    private static final String PK = "PK";
    private static final String SK = "SK";

    private static final String GSI1 = "GSI1";
    private static final String GSI1PK = "GSI1PK";
    private static final String GSI1SK = "GSI1SK";
    private static final String GSI2 = "GSI2";
    private static final String GSI2PK = "GSI2PK";
    private static final String GSI2SK = "GSI2SK";

    private static final String TABLE_PREFIX = "Table#";
    private static final String USER_PREFIX = "User#";
    private static final String PLAYER_PREFIX = "Player#";
    private static final String STATE_PREFIX = "State#";
    private static final String GAME_PREFIX = "Game#";
    private static final String LOG_PREFIX = "Log#";

    private static final String VERSION = "Version";
    private static final String TTL = "TTL";

    private static final int MAX_BATCH_WRITE_SIZE = 25;

    private final Games games;
    private final DynamoDbClient client;
    private final DynamoDbConfiguration config;

    @Inject
    public TableDynamoDbRepositoryV2(@NonNull Games games,
                                     @NonNull DynamoDbClient client,
                                     @NonNull DynamoDbConfiguration config) {
        this.games = games;
        this.client = client;
        this.config = config;
    }

    private String gameIdSharded(Game.Id gameId, Table.Id tableId) {
        return GAME_PREFIX + gameId.getId() + "#" + Math.abs(tableId.hashCode()) % config.getWriteGameIdShards();
    }

    @Override
    public Optional<Table> findById(Table.Id id) {
        var response = client.getItem(GetItemRequest.builder()
                .tableName(config.getTableName())
                .key(Map.of(
                        PK, Item.s(TABLE_PREFIX + id.getId()),
                        SK, Item.s(TABLE_PREFIX + id.getId())
                ))
                .build());

        if (response.hasItem()) {
            return Optional.of(mapToTable(List.of(Item.of(response.item()))));
        }

        return Optional.empty();
    }

    @Override
    public void add(Table table) {
        if (countActive(table.getOwnerId()) >= MAX_ACTIVE_GAMES) {
            throw new ExceedsMaxActiveGames();
        }

        put(table);
    }

    public void put(Table table) {
        Stream.of(
                Stream.of(WriteRequest.builder()
                        .putRequest(PutRequest.builder()
                                .item(mapItemFromTable(table).asMap())
                                .build())
                        .build()),

                table.getPlayers().stream()
                        // Adjacency items only needed for human players,
                        // since computers will never query the index
                        .filter(Player::isUser)
                        .map(player -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(mapItemFromPlayer(player, table))
                                        .build())
                                .build()),

                table.getCurrentState().get().stream()
                        .map(currentState -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(mapItemFromState(table.getId(),
                                                table.getGame(), currentState.getState(), currentState.getTimestamp(),
                                                currentState.getPrevious().get().map(Table.HistoricState::getTimestamp)))
                                        .build())
                                .build()),

                table.getLog().stream()
                        .map(logEntry -> WriteRequest.builder()
                                .putRequest(PutRequest.builder()
                                        .item(mapItemFromLogEntry(logEntry, table.getId()).asMap())
                                        .build())
                                .build())
        )
                .flatMap(Function.identity())
                .collect(Chunked.chunked(MAX_BATCH_WRITE_SIZE))
                .forEach(chunk ->
                        client.batchWriteItem(BatchWriteItemRequest.builder()
                                .requestItems(Map.of(config.getTableName(), chunk))
                                .build()));
    }

    public Item mapItemFromLogEntry(LogEntry logEntry, Table.Id tableId) {
        return new Item()
                .setString(PK, TABLE_PREFIX + tableId.getId())
                .setString(SK, LOG_PREFIX + logEntry.getTimestamp().toString())
                .setString("UserId", logEntry.getUserId().map(User.Id::getId).orElse(null))
                .setString("PlayerId", logEntry.getPlayerId().getId())
                .setEnum("Type", logEntry.getType())
                .set("Parameters", AttributeValue.builder()
                        .l(logEntry.getParameters().stream()
                                .map(Item::s)
                                .collect(Collectors.toList()))
                        .build());
    }

    @Override
    public void update(Table table) {
        if (table.getCurrentState().isResolved()) {
            table.getCurrentState().get()
                    .filter(Table.CurrentState::isChanged)
                    .ifPresent(currentState -> addState(table.getId(),
                            table.getGame(), currentState.getState(), currentState.getTimestamp(),
                            currentState.getPrevious().get().map(Table.HistoricState::getTimestamp)));
        }

        var updateItem = new UpdateItem()
                .setInt(VERSION, table.getVersion() + 1)
                .setEnum("Type", table.getType())
                .setEnum("Mode", table.getMode())
                .setEnum("Visibility", table.getVisibility())
                .setEnum("Status", table.getStatus())
                .setInstant("Updated", table.getUpdated())
                .setTTL(TTL, table.getExpires().orElse(null))
                .setString("OwnerId", table.getOwnerId().getId())
                .set("Players", AttributeValue.builder().l(table.getPlayers().stream()
                        .map(this::mapFromPlayer)
                        .collect(Collectors.toList()))
                        .build())
                .set("Options", mapFromOptions(table.getOptions()));

        if (table.getStarted() != null) {
            updateItem.setInstant("Started", table.getStarted());
        }

        if (table.getEnded() != null) {
            updateItem.setInstant("Ended", table.getEnded());
        }

        if (table.getStatus() != Table.Status.ABANDONED) {
            updateItem.setString(GSI1SK, GSI1SortKey.fromTable(table));

            if (table.isActive()) {
                updateItem.setString(GSI2SK, GSI2SortKey.fromTable(table));
            } else {
                updateItem.remove(GSI2PK, GSI2SK);
            }
        } else {
            updateItem.remove(GSI1PK, GSI1SK, GSI2PK, GSI2SK);
        }

        updateItem.expressionAttributeValue(":ExpectedVersion", Item.n(table.getVersion()));

        var request = UpdateItemRequest.builder()
                .tableName(config.getTableName())
                .key(Map.of(
                        PK, Item.s(TABLE_PREFIX + table.getId().getId()),
                        SK, Item.s(TABLE_PREFIX + table.getId().getId())
                ))
                .conditionExpression(VERSION + "=:ExpectedVersion")
                .updateExpression(updateItem.getUpdateExpression())
                .expressionAttributeNames(updateItem.getExpressionAttributeNames())
                .expressionAttributeValues(updateItem.getExpressionAttributeValues())
                .build();

        try {
            client.updateItem(request);
        } catch (ConditionalCheckFailedException e) {
            throw new Repository.ConcurrentModificationException(e);
        }

        updatePlayerItems(table);

        addLogEntries(table);
    }

    public void addState(@NonNull Table.Id tableId,
                         @NonNull Game game, @NonNull State state, @NonNull Instant timestamp,
                         @NonNull Optional<Instant> previousTimestamp) {
        client.putItem(PutItemRequest.builder()
                .tableName(config.getTableName())
                .item(mapItemFromState(tableId, game, state, timestamp, previousTimestamp))
                .build());
    }

    private void addLogEntries(Table table) {
        var log = table.getLog();

        var logEntries = log instanceof LazyLog
                ? ((LazyLog) log).pending()
                : log.stream();

        var writeRequests = logEntries
                .map(logEntry -> mapItemFromLogEntry(logEntry, table.getId()))
                .map(logItem -> WriteRequest.builder()
                        .putRequest(PutRequest.builder()
                                .item(logItem.asMap())
                                .build())
                        .build())
                .collect(Collectors.toList());

        if (!writeRequests.isEmpty()) {
            client.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(config.getTableName(), writeRequests))
                    .build());
        }
    }

    private int countActive(User.Id userId) {
        return client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI2)
                .keyConditionExpression("GSI2PK=:GSI2PK AND begins_with(GSI2SK, :GSI2SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI2PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI2SK", Item.s(TABLE_PREFIX)
                ))
                .select(Select.COUNT)
                .build())
                .count();
    }

    @Override
    public Stream<Table> findActive(User.Id userId) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI2)
                .scanIndexForward(false)
                .keyConditionExpression("GSI2PK=:GSI2PK AND begins_with(GSI2SK, :GSI2SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI2PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI2SK", Item.s(TABLE_PREFIX)
                ))
                .build())
                .items().stream()
                .map(item -> GSI2SortKey.toTableId(item.get(GSI2SK).s()))
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    @Override
    public Stream<Table> findRecent(User.Id userId, int maxResults) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI1)
                .scanIndexForward(false)
                .keyConditionExpression("GSI1PK=:GSI1PK AND begins_with(GSI1SK, :GSI1SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI1PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI1SK", Item.s(TABLE_PREFIX)
                ))
                .build())
                .items().stream()
                .limit(maxResults)
                .map(item -> GSI1SortKey.parse(item.get(GSI1SK).s()))
                .map(GSI1SortKey::getTableId)
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    @Override
    public Stream<Table> findRecent(User.Id userId, Game.Id gameId, int maxResults) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI1)
                .scanIndexForward(false)
                .keyConditionExpression("GSI1PK=:GSI1PK AND begins_with(GSI1SK, :GSI1SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI1PK", Item.s(USER_PREFIX + userId.getId()),
                        ":GSI1SK", Item.s(TABLE_PREFIX)
                ))
                .build())
                .items().stream()
                .map(item -> GSI1SortKey.parse(item.get(GSI1SK).s()))
                .filter(sk -> sk.getGameId().equals(gameId))
                .limit(maxResults)
                .map(GSI1SortKey::getTableId)
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    @Override
    public Stream<Table> findAll(Game.Id gameId, int maxResults) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .indexName(GSI1)
                .scanIndexForward(false)
                .keyConditionExpression("GSI1PK=:GSI1PK AND begins_with(GSI1SK, :GSI1SK)")
                .expressionAttributeValues(Map.of(
                        ":GSI1PK", Item.s(GAME_PREFIX + gameId.getId()),
                        ":GSI1SK", Item.s(TABLE_PREFIX)
                ))
                .build())
                .items().stream()
                .limit(maxResults)
                .map(item -> GSI1SortKey.parse(item.get(GSI1SK).s()))
                .map(GSI1SortKey::getTableId)
                .map(this::findById)
                .flatMap(Optional::stream);
    }

    private void updatePlayerItems(Table table) {
        var trackingSet = (TrackingSet<Player>) table.getPlayers();

        if (!trackingSet.getAdded().isEmpty() || !trackingSet.getRemoved().isEmpty()) {
            var writeRequests = Stream.concat(
                    trackingSet.getRemoved().stream()
                            .filter(Player::isUser)
                            .map(player -> WriteRequest.builder()
                                    .deleteRequest(DeleteRequest.builder()
                                            .key(Map.of(
                                                    PK, Item.s(TABLE_PREFIX + table.getId().getId()),
                                                    SK, Item.s(PLAYER_PREFIX + player.getId().getId())
                                            ))
                                            .build())
                                    .build()),
                    trackingSet.getAdded().stream()
                            .filter(Player::isUser)
                            .map(player -> WriteRequest.builder()
                                    .putRequest(PutRequest.builder()
                                            .item(mapItemFromPlayer(player, table))
                                            .build())
                                    .build()))
                    .collect(Collectors.toList());

            if (!writeRequests.isEmpty()) {
                client.batchWriteItem(BatchWriteItemRequest.builder()
                        .requestItems(Map.of(config.getTableName(), writeRequests))
                        .build());
            }
        }

        trackingSet.getNotAddedOrRemoved()
                .filter(Player::isUser)
                .forEach(player -> updatePlayerItem(player, table));

        trackingSet.flush();
    }

    private void updatePlayerItem(Player player, Table table) {
        var updateItem = new UpdateItem();

        updateItem.setTTL(TTL, table.getExpires().orElse(null));

        player.getUserId().ifPresentOrElse(userId -> {
            updateItem.setString(GSI1SK, GSI1SortKey.fromTable(table));

            if (table.isActive()) {
                updateItem.setString(GSI2SK, GSI2SortKey.fromTable(table));
            } else {
                updateItem.remove(GSI2PK, GSI2SK);
            }
        }, () -> updateItem.remove(GSI1PK, GSI1SK, GSI2PK, GSI2SK));

        client.updateItem(UpdateItemRequest.builder()
                .tableName(config.getTableName())
                .key(Map.of(
                        PK, Item.s(TABLE_PREFIX + table.getId().getId()),
                        SK, Item.s(PLAYER_PREFIX + player.getId().getId())))
                .updateExpression(updateItem.getUpdateExpression())
                .expressionAttributeNames(updateItem.getExpressionAttributeNames())
                .expressionAttributeValues(updateItem.getExpressionAttributeValues())
                .build());
    }

    private Item mapItemFromTable(Table table) {
        var item = new Item()
                .setString(PK, TABLE_PREFIX + table.getId().getId())
                .setString(SK, TABLE_PREFIX + table.getId().getId())
                .setInt(VERSION, table.getVersion());

        if (table.getStatus() != Table.Status.ABANDONED) {
            item.setString(GSI1PK, gameIdSharded(table.getGame().getId(), table.getId()))
                    .setString(GSI1SK, GSI1SortKey.fromTable(table));
        }

        if (table.isActive()) {
            item.setString(GSI2PK, gameIdSharded(table.getGame().getId(), table.getId()));
            item.setString(GSI2SK, GSI2SortKey.fromTable(table));
        }

        item.setString("GameId", table.getGame().getId().getId())
                .setEnum("Type", table.getType())
                .setEnum("Mode", table.getMode())
                .setEnum("Visibility", table.getVisibility())
                .setEnum("Status", table.getStatus())
                .set("Options", mapFromOptions(table.getOptions()))
                .setInstant("Created", table.getCreated())
                .setInstant("Updated", table.getUpdated());
        if (table.getStarted() != null) {
            item.setInstant("Started", table.getStarted());
        }
        if (table.getEnded() != null) {
            item.setInstant("Ended", table.getEnded());
        }
        item.setTTL(TTL, table.getExpires().orElse(null))
                .setString("OwnerId", table.getOwnerId().getId())
                .set("Players", AttributeValue.builder()
                        .l(table.getPlayers().stream()
                                .map(this::mapFromPlayer)
                                .collect(Collectors.toList()))
                        .build());

        return item;
    }

    private Map<String, AttributeValue> mapItemFromPlayer(Player player, Table table) {
        var item = new Item();

        item.setString(PK, TABLE_PREFIX + table.getId().getId());
        item.setString(SK, PLAYER_PREFIX + player.getId().getId());

        player.getUserId().ifPresent(userId -> {
            item.setString(GSI1PK, USER_PREFIX + userId.getId());
            item.setString(GSI1SK, GSI1SortKey.fromTable(table));

            if (table.isActive()) {
                item.setString(GSI2PK, USER_PREFIX + userId.getId());
                item.setString(GSI2SK, GSI2SortKey.fromTable(table));
            }
        });

        item.setTTL(TTL, table.getExpires().orElse(null));

        return item.asMap();
    }

    private Map<String, AttributeValue> mapItemFromState(Table.Id tableId,
                                                         Game game, State state, Instant timestamp,
                                                         Optional<Instant> previousTimestamp) {
        var stateSerializer = game.getProvider().getStateSerializer();
        var serialized = DynamoDbJson.toJson(jsonBuilderFactory ->
                stateSerializer.serialize(state, jsonBuilderFactory));

        return mapItemFromState(tableId, timestamp, previousTimestamp, serialized);
    }

    public static Map<String, AttributeValue> mapItemFromState(Table.Id tableId,
                                                               Instant timestamp,
                                                               Optional<Instant> previousTimestamp,
                                                               AttributeValue state) {
        var item = new HashMap<String, AttributeValue>();
        item.put(PK, Item.s(TABLE_PREFIX + tableId.getId()));
        item.put(SK, Item.s(STATE_PREFIX + timestamp.toString()));
        item.put("Timestamp", Item.s(timestamp.toString()));

        previousTimestamp.map(Item::s).ifPresent(p -> item.put("Previous", p));

        item.put("State", state);
        return item;
    }

    private AttributeValue mapFromPlayer(Player player) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Id", Item.s(player.getId().getId()));
        map.put("Type", Item.s(player.getType().name()));
        map.put("UserId", player.getUserId().map(userId -> Item.s(userId.getId())).orElse(null));
        map.put("Status", Item.s(player.getStatus().name()));
        map.put("Color", player.getColor() != null ? Item.s(player.getColor().name()) : null);
        map.put("Score", player.getScore().map(score -> AttributeValue.builder().n(Integer.toString(score)).build()).orElse(null));
        map.put("Winner", player.getWinner().map(winner -> AttributeValue.builder().bool(winner).build()).orElse(null));
        map.put("Created", Item.s(player.getCreated().toString()));
        map.put("Updated", Item.s(player.getUpdated().toString()));
        map.put("Turn", AttributeValue.builder().bool(player.isTurn()).build());
        map.put("TurnLimit", player.getTurnLimit().map(turnLimit -> Item.s(turnLimit.toString())).orElse(null));
        return AttributeValue.builder().m(map).build();
    }

    private AttributeValue mapFromOptions(Options options) {
        return AttributeValue.builder().m(options.asMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue() instanceof Boolean) {
                        return AttributeValue.builder().bool((Boolean) entry.getValue()).build();
                    } else if (entry.getValue() instanceof Number) {
                        return AttributeValue.builder().n(entry.getValue().toString()).build();
                    } else {
                        return Item.s(entry.getValue().toString());
                    }
                })))
                .build();
    }

    private Table mapToTable(List<Item> items) {
        var item = items.get(0);

        var id = Table.Id.of(item.getString(PK).replace(TABLE_PREFIX, ""));

        var players = item.get("Players").l().stream()
                .map(this::mapToPlayer)
                .collect(Collectors.toCollection(TrackingSet::new));
        players.flush(); // Consider everything added up until now to be unchanged

        var game = games.get(Game.Id.of(item.getString("GameId")));

        return Table.builder()
                .id(id)
                .version(item.getInt(VERSION))
                .type(item.getEnum("Type", Table.Type.class))
                .mode(item.getEnum("Mode", Table.Mode.class))
                .visibility(item.getOptionalEnum("Visibility", Table.Visibility.class).orElse(Table.Visibility.PRIVATE))
                .game(game)
                .status(item.getEnum("Status", Table.Status.class))
                .options(mapToOptions(item.get("Options")))
                .created(item.getInstant("Created"))
                .updated(item.getInstant("Updated"))
                .started(item.getOptionalInstant("Started").orElse(null))
                .ended(item.getOptionalInstant("Ended").orElse(null))
                .ownerId(User.Id.of(item.getString("OwnerId")))
                .players(players)
                .currentState(items.size() > 1 ? Lazy.of(Optional.of(mapToCurrentState(id, items.get(1), game))) :
                        Lazy.defer(() -> getCurrentState(id, game)))
                .log(new LazyLog((since, before, limit) -> getLogEntries(id, since, before, limit)))
                .build();
    }

    private Table.CurrentState mapToCurrentState(Table.Id tableId, Item item, Game game) {
        return Table.CurrentState.builder()
                .state(mapToState(game, item.get("State")))
                .timestamp(Instant.parse(item.get("Timestamp").s()))
                .previous(Optional.ofNullable(item.get("Previous"))
                        .map(AttributeValue::s)
                        .map(Instant::parse)
                        .map(previous -> Lazy.defer(() -> getHistoricState(tableId, previous, game)))
                        .orElse(Lazy.of(Optional.empty())))
                .changed(false)
                .build();
    }

    private Optional<Table.CurrentState> getCurrentState(Table.Id tableId, Game game) {
        var response = client.query(QueryRequest.builder()
                .tableName(config.getTableName())
                .keyConditionExpression("PK=:PK AND begins_with(SK,:SK)")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(TABLE_PREFIX + tableId.getId()),
                        ":SK", Item.s(STATE_PREFIX)
                ))
                .scanIndexForward(false)
                .limit(1)
                .build());

        if (response.hasItems() && response.count() > 0) {
            return Optional.of(mapToCurrentState(tableId, Item.of(response.items().get(0)), game));
        }
        return Optional.empty();
    }

    private Optional<Table.HistoricState> getHistoricState(Table.Id tableId, Instant timestamp, Game game) {
        var response = client.getItem(GetItemRequest.builder()
                .tableName(config.getTableName())
                .key(Map.of(
                        PK, Item.s(TABLE_PREFIX + tableId.getId()),
                        SK, Item.s(STATE_PREFIX + timestamp.toString())
                ))
                .build());

        if (response.hasItem()) {
            var item = response.item();

            return Optional.of(Table.HistoricState.builder()
                    .state(mapToState(game, item.get("State")))
                    .timestamp(Instant.parse(item.get("Timestamp").s()))
                    .previous(Optional.ofNullable(item.get("Previous"))
                            .map(AttributeValue::s)
                            .map(Instant::parse)
                            .map(previous -> Lazy.defer(() -> getHistoricState(tableId, previous, game)))
                            .orElse(Lazy.of(Optional.empty())))
                    .build());
        }

        return Optional.empty();
    }

    private Stream<LogEntry> getLogEntries(Table.Id tableId, Instant since, Instant before, int limit) {
        return client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                // Needs to put an upper limit on SK because else it will also return other items like "Player#..." and "State#..."
                .keyConditionExpression("PK=:PK AND SK BETWEEN :SKFrom AND :SKTo")
                .expressionAttributeValues(Map.of(
                        ":PK", Item.s(TABLE_PREFIX + tableId.getId()),
                        ":SKFrom", Item.s(LOG_PREFIX + since.toString()),
                        ":SKTo", Item.s(LOG_PREFIX + before.toString())
                ))
                .scanIndexForward(false)
                .limit(Math.min(9999, limit) + 2) // Retrieve 2 more because BETWEEN is inclusive on both ends
                .build())
                .items().stream()
                .map(Item::of)
                .map(this::mapToLogEntry)
                // Filter because BETWEEN is inclusive on both ends
                .filter(logEntry -> logEntry.getTimestamp().isAfter(since) && logEntry.getTimestamp().isBefore(before))
                .limit(limit);
    }

    private LogEntry mapToLogEntry(Item item) {
        return LogEntry.builder()
                .timestamp(Instant.parse(item.getString("SK").replace(LOG_PREFIX, "")))
                .playerId(Player.Id.of(item.getString("PlayerId")))
                .userId(item.getOptionalString("UserId")
                        .map(User.Id::of)
                        .orElse(null))
                .type(item.getEnum("Type", LogEntry.Type.class))
                .parameters(item.get("Parameters").l().stream()
                        .map(AttributeValue::s)
                        .collect(Collectors.toList()))
                .build();
    }

    private State mapToState(Game game, AttributeValue attributeValue) {
        return DynamoDbJson.fromJson(attributeValue, game.getProvider().getStateDeserializer()::deserialize);
    }

    private Options mapToOptions(AttributeValue attributeValue) {
        return new Options(attributeValue.m().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue().bool() != null) {
                        return entry.getValue().bool();
                    } else if (entry.getValue().n() != null) {
                        return Float.parseFloat(entry.getValue().n());
                    } else {
                        return entry.getValue().s();
                    }
                })));
    }

    private Player mapToPlayer(AttributeValue attributeValue) {
        Map<String, AttributeValue> map = attributeValue.m();
        return Player.builder()
                .id(Player.Id.of(map.get("Id").s()))
                .type(map.containsKey("Type") ? Player.Type.valueOf(map.get("Type").s()) : Player.Type.USER)
                .userId(map.containsKey("UserId") ? User.Id.of(map.get("UserId").s()) : null)
                .status(Player.Status.valueOf(map.get("Status").s()))
                .color(map.containsKey("Color") ? PlayerColor.valueOf(map.get("Color").s()) : null)
                .score(map.containsKey("Score") ? Integer.parseInt(map.get("Score").n()) : null)
                .winner(map.containsKey("Winner") ? map.get("Winner").bool() : null)
                .created(Instant.parse(map.get("Created").s()))
                .updated(Instant.parse(map.get("Updated").s()))
                .turn(map.containsKey("Turn") ? map.get("Turn").bool() : false)
                .turnLimit(map.containsKey("TurnLimit") ? Instant.parse(map.get("TurnLimit").s()) : null)
                .build();
    }

    public void delete(Table.Id id) {
        client.queryPaginator(QueryRequest.builder()
                .tableName(config.getTableName())
                .keyConditionExpression("PK=:PK")
                .expressionAttributeValues(Map.of(":PK", Item.s(id.getId())))
                .projectionExpression("PK,SK")
                .build())
                .items()
                .stream()
                .map(key -> WriteRequest.builder()
                        .deleteRequest(DeleteRequest.builder()
                                .key(key)
                                .build())
                        .build())
                .collect(Chunked.chunked(MAX_BATCH_WRITE_SIZE))
                .forEach(chunk ->
                        client.batchWriteItem(BatchWriteItemRequest.builder()
                                .requestItems(Map.of(config.getTableName(), chunk))
                                .build()));
    }

    @Value
    private static class GSI1SortKey {

        Table.Id tableId;
        Game.Id gameId;

        static String fromTable(Table table) {
            if (table.getEnded() != null) {
                return TABLE_PREFIX + table.getEnded().toString() + "#" + table.getGame().getId().getId() + "#" + table.getId().getId();
            } else if (table.getStarted() != null) {
                return TABLE_PREFIX + table.getStarted().toString() + "#" + table.getGame().getId().getId() + "#" + table.getId().getId();
            } else {
                return TABLE_PREFIX + table.getCreated().toString() + "#" + table.getGame().getId().getId() + "#" + table.getId().getId();
            }
        }

        public static GSI1SortKey parse(String sk) {
            if (!sk.startsWith(TABLE_PREFIX)) {
                throw new IllegalArgumentException("GSI1SK does not contain table ID: " + sk);
            }
            var parts = sk.split("#");
            return new GSI1SortKey(Table.Id.of(parts[3]), Game.Id.of(parts[2]));
        }

    }

    private static class GSI2SortKey {
        static String fromTable(Table table) {
            if (table.canAnyoneJoin()) {
                return TABLE_PREFIX + "OPEN#" + table.getStarted().toString() + "#" + table.getId().getId();
            } else {
                switch (table.getStatus()) {
                    case NEW:
                        return TABLE_PREFIX + "NEW#" + table.getCreated().toString() + "#" + table.getId().getId();
                    case STARTED:
                        return TABLE_PREFIX + "ASTARTED#" + table.getStarted().toString() + "#" + table.getId().getId();
                    case ENDED:
                        return TABLE_PREFIX + table.getEnded().toString() + "#" + table.getId().getId();
                    default:
                        throw new IllegalStateException(String.format("Table '%s' not in valid state for GSI2SK: %s" + table.getId().getId(), table.getStatus()));
                }
            }
        }

        static Table.Id toTableId(String sk) {
            if (!sk.startsWith(TABLE_PREFIX)) {
                throw new IllegalArgumentException("GSI2SK does not contain table ID: " + sk);
            }
            var parts = sk.split("#");
            return Table.Id.of(parts[parts.length - 1]);
        }
    }
}
