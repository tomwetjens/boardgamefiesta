package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.*;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.dynamodb.json.DynamoDbJson;
import lombok.NonNull;
import lombok.Value;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TableDynamoDbRepository implements Tables {

    private static final String TABLE_NAME = "gwt-games";
    private static final String LOG_TABLE_NAME = "gwt-log";
    private static final String STATE_TABLE_NAME = "gwt-state";

    private static final String USER_ID_ID_INDEX = "UserId-Id-index";
    private static final String USER_ID_CREATED_INDEX = "UserId-Created-index";

    private static final int FIRST_VERSION = 1;

    private final Games games;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String logTableName;
    private final String historicStateTableName;

    @Inject
    public TableDynamoDbRepository(@NonNull Games games,
                                   @NonNull DynamoDbClient dynamoDbClient,
                                   @NonNull DynamoDbConfiguration config) {
        this.games = games;
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = TABLE_NAME + config.getTableSuffix().orElse("");
        this.logTableName = LOG_TABLE_NAME + config.getTableSuffix().orElse("");
        this.historicStateTableName = STATE_TABLE_NAME + config.getTableSuffix().orElse("");
    }

    @Override
    public Optional<Table> findById(Table.Id id) {
        return getItem(id, false);
    }

    @Override
    public Stream<Table> findActive(User.Id userId) {
        return getTables(getActiveTableIds(userId)).map(this::mapToTable);
    }

    @Override
    public Stream<Table> findRecent(User.Id userId, int maxResults) {
        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_CREATED_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .filterExpression("#Status <> :Abandoned")
                .expressionAttributeNames(Map.of(
                        "#Status", "Status"))
                .expressionAttributeValues(Map.of(
                        ":UserId", AttributeValue.builder().s("User-" + userId.getId()).build(),
                        ":Abandoned", AttributeValue.builder().s(Table.Status.ABANDONED.name()).build()))
                .scanIndexForward(false) // Most recent first
                .limit(maxResults)
                .build())
                .items().stream()
                .limit(maxResults)
                .map(item -> Table.Id.of(item.get("Id").s()))
                .flatMap(tableId -> findOptionallyById(tableId).stream());
    }

    @Override
    public Stream<Table> findRecent(User.Id userId, Game.Id gameId, int maxResults) {
        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_CREATED_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .filterExpression("GameId = :GameId AND #Status <> :Abandoned")
                .expressionAttributeNames(Map.of(
                        "#Status", "Status"))
                .expressionAttributeValues(Map.of(
                        ":UserId", AttributeValue.builder().s("User-" + userId.getId()).build(),
                        ":GameId", AttributeValue.builder().s(gameId.getId()).build(),
                        ":Abandoned", AttributeValue.builder().s(Table.Status.ABANDONED.name()).build()))
                .scanIndexForward(false) // Most recent first
                .limit(maxResults)
                .build())
                .items().stream()
                .limit(maxResults)
                .map(item -> Table.Id.of(item.get("Id").s()))
                .flatMap(tableId -> findOptionallyById(tableId).stream());
    }

    @Value
    private class TableSummary {
        Table.Id id;
        Instant ended;
    }

    public Stream<Table> findAllEndedSortedByEndedAscending() {
        return dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(tableName)
                // Filter out the adjacency list items
                .filterExpression("begins_with(UserId, :UserIdBeginsWith) and #Status = :Ended")
                .expressionAttributeNames(Map.of(
                        "#Status", "Status"
                ))
                .expressionAttributeValues(Map.of(
                        ":UserIdBeginsWith", AttributeValue.builder().s("Table-").build(),
                        ":Ended", AttributeValue.builder().s(Table.Status.ENDED.name()).build()
                ))
                .projectionExpression("Id, Ended")
                .build())
                .items().stream()
                .map(item -> new TableSummary(Table.Id.of(item.get("Id").s()), Instant.ofEpochSecond(Long.parseLong(item.get("Ended").n()))))
                .sorted(Comparator.comparing(TableSummary::getEnded))
                .flatMap(tableSummary -> findById(tableSummary.getId()).stream());
    }

    @Override
    public Stream<Table> findAll(Game.Id gameId, int maxResults) {
        return dynamoDbClient.scanPaginator(ScanRequest.builder()
                .tableName(tableName)
                // Filter out the adjacency list items
                .filterExpression("GameId = :GameId AND begins_with(UserId, :UserIdBeginsWith)")
                .expressionAttributeValues(Map.of(
                        ":GameId", AttributeValue.builder().s(gameId.getId()).build(),
                        ":UserIdBeginsWith", AttributeValue.builder().s("Table-").build()))
                .limit(maxResults)
                .build())
                .items().stream()
                .map(this::mapToTable);
    }

    private Stream<Map<String, AttributeValue>> getTables(Set<Table.Id> ids) {
        if (ids.isEmpty()) {
            return Stream.empty();
        }
        return dynamoDbClient.batchGetItemPaginator(BatchGetItemRequest.builder()
                // Max 100 items, max 16 MB!!
                .requestItems(Map.of(tableName, KeysAndAttributes.builder()
                        .consistentRead(true)
                        .keys(ids.stream()
                                .map(id -> Map.of(
                                        "Id", AttributeValue.builder().s(id.getId()).build(),
                                        "UserId", AttributeValue.builder().s("Table-" + id.getId()).build()))
                                .collect(Collectors.toSet()))
                        .build()))
                .build())
                .stream()
                .map(BatchGetItemResponse::responses)
                .flatMap(response -> response.get(tableName).stream());
    }

    private Set<Table.Id> getActiveTableIds(User.Id userId) {
        return dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .filterExpression("Active = :Active")
                .expressionAttributeValues(Map.of(
                        ":UserId", AttributeValue.builder().s("User-" + userId.getId()).build(),
                        ":Active", AttributeValue.builder().bool(true).build()))
                .build())
                .items().stream()
                .map(item -> Table.Id.of(item.get("Id").s()))
                .collect(Collectors.toSet());
    }

    private int countActive(User.Id userId) {
        return getActiveTableIds(userId).size();
    }

    private int countActiveByType(User.Id userId, Table.Type type) {
        return (int) getTables(getActiveTableIds(userId))
                .filter(item -> Table.Type.valueOf(item.get("Type").s()) == type)
                .count();
    }

    @Override
    public void add(Table table) {
        if (countActiveByType(table.getOwnerId(), Table.Type.REALTIME) >= MAX_ACTIVE_REALTIME_GAMES) {
            throw new ExceedsMaxRealtimeGames();
        }

        if (countActive(table.getOwnerId()) >= MAX_ACTIVE_GAMES) {
            throw new ExceedsMaxActiveGames();
        }

        var item = new HashMap<>(key(table.getId()));
        item.put("Version", AttributeValue.builder().n(Integer.toString(table.getVersion())).build());
        item.put("GameId", AttributeValue.builder().s(table.getGame().getId().getId()).build());
        item.put("Type", AttributeValue.builder().s(table.getType().name()).build());
        item.put("Mode", AttributeValue.builder().s(table.getMode().name()).build());
        item.put("Visibility", AttributeValue.builder().s(table.getVisibility().name()).build());
        item.put("Status", AttributeValue.builder().s(table.getStatus().name()).build());
        item.put("Options", mapFromOptions(table.getOptions()));
        item.put("Created", AttributeValue.builder().n(Long.toString(table.getCreated().getEpochSecond())).build());
        item.put("Updated", AttributeValue.builder().n(Long.toString(table.getUpdated().getEpochSecond())).build());
        item.put("Started", table.getStarted() != null ? AttributeValue.builder().n(Long.toString(table.getStarted().getEpochSecond())).build() : null);
        item.put("Ended", table.getEnded() != null ? AttributeValue.builder().n(Long.toString(table.getEnded().getEpochSecond())).build() : null);
        item.put("Expires", AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build());
        item.put("OwnerId", AttributeValue.builder().s(table.getOwnerId().getId()).build());
        item.put("Players", AttributeValue.builder().l(table.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build());

        table.getCurrentState().get().ifPresent(currentState -> {
            item.put("State", addState(table, currentState));
            item.put("StateTimestamp", AttributeValue.builder().n(Long.toString(currentState.getTimestamp().toEpochMilli())).build());
            currentState.getPrevious().get().ifPresent(previous ->
                    item.put("PreviousStateTimestamp", AttributeValue.builder().n(Long.toString(previous.getTimestamp().toEpochMilli())).build()));

            // TODO Add historic state recursively
        });

        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(
                        tableName, Stream.concat(
                                Stream.of(WriteRequest.builder().putRequest(
                                        PutRequest.builder()
                                                .item(item)
                                                .build())
                                        .build()),
                                table.getPlayers().stream()
                                        .filter(player -> player.getType() == Player.Type.USER)
                                        .map(player -> mapToAdjacencyListItem(table, player))
                                        .map(lookupItem -> WriteRequest.builder()
                                                .putRequest(PutRequest.builder()
                                                        .item(lookupItem)
                                                        .build())
                                                .build()))
                                .collect(Collectors.toList())))
                .build());

        addLogEntries(table);
    }

    private AttributeValue addState(Table table, Table.CurrentState currentState) {
        var serialized = mapFromState(table.getGame(), currentState.getState());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(historicStateTableName)
                .item(Map.of(
                        "TableId", AttributeValue.builder().s(table.getId().getId()).build(),
                        "Timestamp", AttributeValue.builder().n(Long.toString(currentState.getTimestamp().toEpochMilli())).build(),
                        "Previous", currentState.getPrevious().get()
                                .map(previous -> AttributeValue.builder().n(Long.toString(previous.getTimestamp().toEpochMilli())).build())
                                .orElse(AttributeValue.builder().nul(true).build()),
                        "GameId", AttributeValue.builder().s(table.getGame().getId().getId()).build(),
                        "Expires", AttributeValue.builder().n(Long.toString(currentState.getExpires().getEpochSecond())).build(),
                        "State", serialized))
                .build());
        // TODO Mark CurrentState as persisted

        return serialized;
    }

    private void addLogEntries(Table table) {
        var log = table.getLog();

        var logEntries = log instanceof LazyLog
                ? ((LazyLog) log).pending()
                : log.stream();

        List<WriteRequest> writeRequests = logEntries
                .map(logEntry -> mapFromLogEntry(table.getId(), logEntry))
                .map(logItem -> WriteRequest.builder()
                        .putRequest(PutRequest.builder()
                                .item(logItem)
                                .build())
                        .build())
                .collect(Collectors.toList());

        if (!writeRequests.isEmpty()) {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(
                            logTableName, writeRequests))
                    .build());
        }
    }

    @Override
    public void update(Table table) throws ConcurrentModificationException {
        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        var expressionAttributeNames = new HashMap<String, String>();
        expressionAttributeNames.put("#Status", "Status");
        expressionAttributeNames.put("#Type", "Type");
        expressionAttributeNames.put("#Mode", "Mode");

        var updateExpression = "SET Version=:Version" +
                ",#Type=:Type" +
                ",#Mode=:Mode" +
                ",Visibility=:Visibility" +
                ",#Status=:Status" +
                ",Updated=:Updated" +
                ",Expires=:Expires" +
                ",OwnerId=:OwnerId" +
                ",Players=:Players" +
                ",Options=:Options";

        expressionAttributeValues.put(":Version", AttributeValue.builder().n(
                // TODO Version can become required after backwards compatibility period
                Integer.toString(table.getVersion() != null ? table.getVersion() + 1 : FIRST_VERSION)).build());
        expressionAttributeValues.put(":Type", AttributeValue.builder().s(table.getType().name()).build());
        expressionAttributeValues.put(":Mode", AttributeValue.builder().s(table.getMode().name()).build());
        expressionAttributeValues.put(":Visibility", AttributeValue.builder().s(table.getVisibility().name()).build());
        expressionAttributeValues.put(":Status", AttributeValue.builder().s(table.getStatus().name()).build());
        expressionAttributeValues.put(":Updated", AttributeValue.builder().n(Long.toString(table.getUpdated().getEpochSecond())).build());
        expressionAttributeValues.put(":Expires", AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build());
        expressionAttributeValues.put(":OwnerId", AttributeValue.builder().s(table.getOwnerId().getId()).build());
        expressionAttributeValues.put(":Players", AttributeValue.builder().l(table.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build());
        expressionAttributeValues.put(":Options", mapFromOptions(table.getOptions()));

        if (table.getStarted() != null) {
            updateExpression += ",Started=:Started";
            expressionAttributeValues.put(":Started", AttributeValue.builder().n(Long.toString(table.getStarted().getEpochSecond())).build());
        }

        if (table.getEnded() != null) {
            updateExpression += ",Ended=:Ended";
            expressionAttributeValues.put(":Ended", AttributeValue.builder().n(Long.toString(table.getEnded().getEpochSecond())).build());
        }

        if (table.getCurrentState().isResolved() && table.getCurrentState().get().isPresent()) {
            var currentState = table.getCurrentState().get().get();

//            if (currentState.isChanged()) {
            updateExpression += ",#State=:State";
            expressionAttributeNames.put("#State", "State");

            expressionAttributeValues.put(":State", addState(table, currentState));

            updateExpression += ",StateTimestamp=:StateTimestamp";
            expressionAttributeValues.put(":StateTimestamp", AttributeValue.builder().n(Long.toString(currentState.getTimestamp().toEpochMilli())).build());

            if (currentState.getPrevious().isResolved()) {
                var previous = currentState.getPrevious().get();
                if (previous.isPresent()) {
                    updateExpression += ",PreviousStateTimestamp=:PreviousStateTimestamp";
                    expressionAttributeValues.put(":PreviousStateTimestamp", AttributeValue.builder().n(Long.toString(previous.get().getTimestamp().toEpochMilli())).build());
                }
            }

            // TODO Add historic states recursively if needed
//            }
        }

        var requestBuilder = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key(table.getId()))
                .updateExpression(updateExpression)
                .expressionAttributeNames(expressionAttributeNames);

        // TODO Version can become required after backwards compatibility period
        if (table.getVersion() != null) {
            requestBuilder = requestBuilder.conditionExpression("Version=:ExpectedVersion");
            expressionAttributeValues.put(":ExpectedVersion", AttributeValue.builder().n(table.getVersion().toString()).build());
        }

        var request = requestBuilder
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        try {
            dynamoDbClient.updateItem(request);
        } catch (ConditionalCheckFailedException e) {
            throw new ConcurrentModificationException(e);
        }

        addLogEntries(table);

        updateAdjacencyList(table);
    }

    private void updateAdjacencyList(Table table) {
        var trackingSet = (TrackingSet<Player>) table.getPlayers();

        if (!trackingSet.getAdded().isEmpty() || !trackingSet.getRemoved().isEmpty()) {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(tableName, Stream.concat(
                            trackingSet.getRemoved().stream()
                                    .map(Player::getUserId)
                                    .flatMap(Optional::stream)
                                    .map(userId -> WriteRequest.builder()
                                            .deleteRequest(DeleteRequest.builder()
                                                    .key(Map.of(
                                                            "Id", AttributeValue.builder().s(table.getId().getId()).build(),
                                                            "UserId", AttributeValue.builder().s("User-" + userId.getId()).build()
                                                    ))
                                                    .build())
                                            .build()),
                            trackingSet.getAdded().stream()
                                    .filter(player -> player.getType() == Player.Type.USER)
                                    .map(player -> WriteRequest.builder()
                                            .putRequest(PutRequest.builder()
                                                    .item(mapToAdjacencyListItem(table, player))
                                                    .build())
                                            .build()))
                            .collect(Collectors.toList())))
                    .build());

            trackingSet.flush();
        }

        trackingSet.getNotAddedOrRemoved()
                .filter(player -> player.getType() == Player.Type.USER)
                .forEach(player -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                        .tableName(tableName)
                        .key(Map.of(
                                "Id", AttributeValue.builder().s(table.getId().getId()).build(),
                                "UserId", AttributeValue.builder().s("User-" + player.getUserId().orElseThrow().getId()).build()
                        ))
                        .attributeUpdates(mapToAdjacencyListItemUpdates(table, player))
                        .build()));
    }

    private Optional<Table> findOptionallyById(Table.Id id) {
        return getItem(id, false);
    }

    private Optional<Table> getItem(Table.Id id, boolean consistentRead) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .consistentRead(consistentRead)
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(mapToTable(response.item()));
    }

    private Map<String, AttributeValue> mapToAdjacencyListItem(Table table, Player player) {
        var item = new HashMap<String, AttributeValue>();

        item.put("Id", AttributeValue.builder().s(table.getId().getId()).build());
        item.put("UserId", AttributeValue.builder().s("User-" + player.getUserId().orElseThrow().getId()).build());

        // Expire adjacency list whenever table expires, since it doesn't make any sense to keep it after
        item.put("Expires", AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build());

        // Having some table attributes redundantly in the adjacency list is important
        // for efficiently querying the active tables a user is in
        item.put("Status", AttributeValue.builder().s(table.getStatus().name()).build());
        item.put("Created", AttributeValue.builder().n(Long.toString(table.getCreated().getEpochSecond())).build());
        item.put("GameId", AttributeValue.builder().s(table.getGame().getId().getId()).build());
        item.put("Active", AttributeValue.builder().bool(table.isActive() && player.isActive()).build());

        return item;
    }

    private Map<String, AttributeValueUpdate> mapToAdjacencyListItemUpdates(Table table, Player player) {
        var map = new HashMap<String, AttributeValueUpdate>();
        map.put("Expires", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build()).build());
        map.put("Status", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(table.getStatus().name()).build()).build());
        map.put("Active", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().bool(table.isActive() && player.isActive()).build()).build());

        return map;
    }

    private AttributeValue mapFromOptions(Options options) {
        return AttributeValue.builder().m(options.asMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue() instanceof Boolean) {
                        return AttributeValue.builder().bool((Boolean) entry.getValue()).build();
                    } else if (entry.getValue() instanceof Number) {
                        return AttributeValue.builder().n(entry.getValue().toString()).build();
                    } else {
                        return AttributeValue.builder().s(entry.getValue().toString()).build();
                    }
                })))
                .build();
    }

    public Table mapToTable(Map<String, AttributeValue> item) {
        var id = Table.Id.of(item.get("Id").s());

        var gameId = Game.Id.of(item.get("GameId").s());
        var game = games.get(gameId);

        Instant updated = Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n()));

        var currentState = Optional.ofNullable(item.get("State"))
                .map(attributeValue -> mapToState(game, attributeValue))
                .map(state -> Table.CurrentState.builder()
                        .state(state)
                        .timestamp(Optional.ofNullable(item.get("StateTimestamp"))
                                .map(AttributeValue::n)
                                .map(Long::parseLong)
                                .map(Instant::ofEpochMilli)
                                .orElse(updated))
                        .previous(Optional.ofNullable(item.get("PreviousStateTimestamp"))
                                .map(AttributeValue::n)
                                .map(Long::parseLong)
                                .map(Instant::ofEpochMilli)
                                .map(previousTimestamp -> Lazy.defer(() -> getHistoricState(game, id, previousTimestamp)))
                                .orElse(Lazy.of(Optional.empty())))
                        .changed(false)
                        .build());

        return Table.builder()
                .id(id)
                // TODO Version can become required after backwards compatibility period
                .version(item.get("Version") != null ? Integer.valueOf(item.get("Version").n()) : null)
                .type(Table.Type.valueOf(item.get("Type").s()))
                .mode(Table.Mode.valueOf(item.get("Mode").s()))
                .visibility(item.get("Visibility") != null ? Table.Visibility.valueOf(item.get("Visibility").s()) : Table.Visibility.PRIVATE)
                .game(game)
                .status(Table.Status.valueOf(item.get("Status").s()))
                .options(new Options(item.get("Options").m().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                            if (entry.getValue().bool() != null) {
                                return entry.getValue().bool();
                            } else if (entry.getValue().n() != null) {
                                return Float.parseFloat(entry.getValue().n());
                            } else {
                                return entry.getValue().s();
                            }
                        }))))
                .created(Instant.ofEpochSecond(Long.parseLong(item.get("Created").n())))
                .updated(updated)
                .started(item.get("Started") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Started").n())) : null)
                .ended(item.get("Ended") != null ? Instant.ofEpochSecond(Long.parseLong(item.get("Ended").n())) : null)
                .ownerId(User.Id.of(item.get("OwnerId").s()))
                .players(item.get("Players").l().stream()
                        .map(av -> mapToPlayer(av, currentState.map(Table.CurrentState::getState)))
                        .collect(Collectors.toCollection(TrackingSet::new)))
                .currentState(Lazy.of(currentState))
                .log(new LazyLog((from, to, limit) -> findLogEntries(id, from, to, limit)))
                .build();
    }

    private State mapToState(Game game, AttributeValue attributeValue) {
        return DynamoDbJson.fromJson(attributeValue, game.getProvider().getStateDeserializer()::deserialize);
    }

    private Optional<Table.HistoricState> getHistoricState(Game game, Table.Id id, Instant timestamp) {
        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":TableId", AttributeValue.builder().s(id.getId()).build());
        expressionAttributeValues.put(":Timestamp", AttributeValue.builder().n(Long.toString(timestamp.toEpochMilli())).build());

        return dynamoDbClient.query(QueryRequest.builder()
                .tableName(historicStateTableName)
                .keyConditionExpression("TableId = :TableId AND #Timestamp = :Timestamp")
                .expressionAttributeNames(Collections.singletonMap("#Timestamp", "Timestamp"))
                .expressionAttributeValues(expressionAttributeValues)
                .build())
                .items().stream()
                .findFirst()
                .map(item -> mapToHistoricState(item, game));
    }

    public Table.HistoricState mapToHistoricState(Map<String, AttributeValue> item, Game game) {
        var tableId = Table.Id.of(item.get("TableId").s());
        return Table.HistoricState.builder()
                .state(mapToState(game, item.get("State")))
                .timestamp(Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n())))
                .previous(Optional.ofNullable(item.get("Previous"))
                        .map(AttributeValue::n)
                        .map(Long::parseLong)
                        .map(Instant::ofEpochMilli)
                        .map(previousTimestamp -> Lazy.defer(() -> getHistoricState(game, tableId, previousTimestamp)))
                        .orElse(Lazy.of(Optional.empty())))
                .build();
    }

    private Player mapToPlayer(AttributeValue attributeValue, Optional<State> state) {
        var map = attributeValue.m();

        var id = Player.Id.of(map.get("Id").s());

        return Player.builder()
                .id(id)
                .type(map.containsKey("Type") ? Player.Type.valueOf(map.get("Type").s()) : Player.Type.USER)
                .userId(map.containsKey("UserId") ? User.Id.of(map.get("UserId").s()) : null)
                .status(Player.Status.valueOf(map.get("Status").s()))
                .color(map.containsKey("Color") ? PlayerColor.valueOf(map.get("Color").s()) : null)
                .score(map.containsKey("Score") ? Integer.parseInt(map.get("Score").n()) : null)
                .winner(map.containsKey("Winner") ? map.get("Winner").bool() : null)
                .created(Instant.ofEpochSecond(Long.parseLong(map.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(map.get("Updated").n())))
                .turn(map.containsKey("Turn") ? map.get("Turn").bool() : state.flatMap(s -> s.getPlayerByName(id.getId()).map(s.getCurrentPlayers()::contains)).orElse(false))
                .turnLimit(map.containsKey("TurnLimit") ? Instant.ofEpochSecond(Long.parseLong(map.get("TurnLimit").n())) : null)
                .build();
    }

    private AttributeValue mapFromState(Game game, State state) {
        var serializer = game.getProvider().getStateSerializer();
        return DynamoDbJson.toJson(jsonBuilderFactory -> serializer.serialize(state, jsonBuilderFactory));
    }

    private AttributeValue mapFromPlayer(Player player) {
        var map = new HashMap<String, AttributeValue>();
        map.put("Id", AttributeValue.builder().s(player.getId().getId()).build());
        map.put("Type", AttributeValue.builder().s(player.getType().name()).build());
        map.put("UserId", player.getUserId().map(userId -> AttributeValue.builder().s(userId.getId()).build()).orElse(null));
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());
        map.put("Color", player.getColor() != null ? AttributeValue.builder().s(player.getColor().name()).build() : null);
        map.put("Score", player.getScore().map(score -> AttributeValue.builder().n(Integer.toString(score)).build()).orElse(null));
        map.put("Winner", player.getWinner().map(winner -> AttributeValue.builder().bool(winner).build()).orElse(null));
        map.put("Created", AttributeValue.builder().n(Long.toString(player.getCreated().getEpochSecond())).build());
        map.put("Updated", AttributeValue.builder().n(Long.toString(player.getUpdated().getEpochSecond())).build());
        map.put("Turn", AttributeValue.builder().bool(player.isTurn()).build());
        map.put("TurnLimit", player.getTurnLimit().map(turnLimit -> AttributeValue.builder().n(Long.toString(turnLimit.getEpochSecond())).build()).orElse(null));
        return AttributeValue.builder().m(map).build();
    }

    private Map<String, AttributeValue> key(Table.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        key.put("UserId", AttributeValue.builder().s("Table-" + id.getId()).build());
        return key;
    }

    private Map<String, AttributeValue> mapFromLogEntry(Table.Id gameId, LogEntry logEntry) {
        var item = new HashMap<String, AttributeValue>();

        item.put("GameId", AttributeValue.builder().s(gameId.getId()).build());
        item.put("Timestamp", AttributeValue.builder().n(Long.toString(logEntry.getTimestamp().toEpochMilli())).build());
        item.put("UserId", logEntry.getUserId().map(userId -> AttributeValue.builder().s(userId.getId()).build()).orElse(null));
        item.put("PlayerId", AttributeValue.builder().s(logEntry.getPlayerId().getId()).build());
        item.put("Type", AttributeValue.builder().s(logEntry.getType().name()).build());
        item.put("Parameters", AttributeValue.builder().l(logEntry.getParameters().stream()
                .map(param -> AttributeValue.builder().s(param).build())
                .collect(Collectors.toList()))
                .build());

        return item;
    }

    public LogEntry mapToLogEntry(Map<String, AttributeValue> item) {
        return LogEntry.builder()
                .timestamp(Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n())))
                .playerId(item.containsKey("PlayerId") ? Player.Id.of(item.get("PlayerId").s()) : null)
                .userId(item.containsKey("UserId") ? User.Id.of(item.get("UserId").s()) : null)
                .type(LogEntry.Type.valueOf(item.get("Type").s()))
                .parameters(item.get("Parameters").l().stream()
                        .map(AttributeValue::s)
                        .collect(Collectors.toList()))
                .build();
    }

    private Stream<LogEntry> findLogEntries(Table.Id gameId, Instant from, Instant to, int limit) {
        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":GameId", AttributeValue.builder().s(gameId.getId()).build());
        expressionAttributeValues.put(":From", AttributeValue.builder().n(Long.toString(from.toEpochMilli())).build());
        expressionAttributeValues.put(":To", AttributeValue.builder().n(Long.toString(to.toEpochMilli())).build());

        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(logTableName)
                .keyConditionExpression("GameId = :GameId AND #Timestamp BETWEEN :From AND :To")
                .expressionAttributeNames(Collections.singletonMap("#Timestamp", "Timestamp"))
                .expressionAttributeValues(expressionAttributeValues)
                .scanIndexForward(false)
                .limit(Math.min(9999, limit) + 2) // Because 'BETWEEN' is inclusive on both ends
                .build())
                .items().stream()
                .map(this::mapToLogEntry)
                // Make from and to exclusive, because 'BETWEEN' is inclusive for both
                .filter(logEntry -> logEntry.getTimestamp().isAfter(from) && logEntry.getTimestamp().isBefore(to))
                .limit(limit);
    }

}
