package com.boardgamefiesta.server.repository;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.server.domain.*;
import com.boardgamefiesta.server.repository.json.DynamoDbJson;
import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TableDynamoDbRepository implements Tables {

    private static final String TABLE_NAME = "gwt-games";
    private static final String LOG_TABLE_NAME = "gwt-log";
    private static final String STATE_TABLE_NAME = "gwt-state";

    private static final String USER_ID_ID_INDEX = "UserId-Id-index";
    public static final int FIRST_VERSION = 1;

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
    public Table findById(Table.Id id) {
        return findOptionallyById(id)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Stream<Table> findByUserId(User.Id userId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(USER_ID_ID_INDEX)
                .keyConditionExpression("UserId = :UserId")
                .expressionAttributeValues(Collections.singletonMap(":UserId", AttributeValue.builder()
                        .s("User-" + userId.getId())
                        .build()))
                .build());

        return response.items().stream()
                .flatMap(item -> findOptionallyById(Table.Id.of(item.get("Id").s())).stream());
    }

    @Override
    public void add(Table table) {
        var serializeCache = Collections.<State, AttributeValue>emptyMap();

        var item = new HashMap<>(key(table.getId()));
        item.put("Version", AttributeValue.builder().n(Integer.toString(table.getVersion())).build());
        item.put("GameId", AttributeValue.builder().s(table.getGame().getId().getId()).build());
        item.put("Type", AttributeValue.builder().s(table.getType().name()).build());
        item.put("Mode", AttributeValue.builder().s(table.getMode().name()).build());
        item.put("Status", AttributeValue.builder().s(table.getStatus().name()).build());
        item.put("Options", mapFromOptions(table.getOptions()));
        item.put("Created", AttributeValue.builder().n(Long.toString(table.getCreated().getEpochSecond())).build());
        item.put("Updated", AttributeValue.builder().n(Long.toString(table.getUpdated().getEpochSecond())).build());
        item.put("Started", table.getStarted() != null ? AttributeValue.builder().n(Long.toString(table.getStarted().getEpochSecond())).build() : null);
        item.put("Ended", table.getEnded() != null ? AttributeValue.builder().n(Long.toString(table.getEnded().getEpochSecond())).build() : null);
        item.put("Expires", AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build());
        item.put("OwnerId", AttributeValue.builder().s(table.getOwnerId().getId()).build());
        item.put("Players", AttributeValue.builder().l(table.getPlayers().stream().map(this::mapFromPlayer).collect(Collectors.toList())).build());

        if (table.getCurrentState().isPresent()) {
            var currentState = table.getCurrentState().get();
            var state = currentState.getState();
            var serialized = mapFromState(table.getGame(), state);

            item.put("State", serialized);
            item.put("StateTimestamp", AttributeValue.builder().n(Long.toString(currentState.getTimestamp().toEpochMilli())).build());
            if (currentState.getPrevious().isPresent()) {
                item.put("PreviousStateTimestamp", AttributeValue.builder().n(Long.toString(currentState.getPrevious().get().toEpochMilli())).build());
            }

            serializeCache = Collections.singletonMap(state, serialized);
        }

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
                                        .map(player -> mapLookupItem(table, player))
                                        .map(lookupItem -> WriteRequest.builder()
                                                .putRequest(PutRequest.builder()
                                                        .item(lookupItem)
                                                        .build())
                                                .build()))
                                .collect(Collectors.toList())))
                .build());

        addPendingHistoricStates(table, serializeCache);
        addLogEntries(table);
    }

    private void addPendingHistoricStates(Table table, Map<State, AttributeValue> serializedCache) {
        if (table.getHistoricStates() != null && table.getHistoricStates().hasPending()) {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(historicStateTableName, table.getHistoricStates().getPending().stream()
                            .map(historicState -> WriteRequest.builder()
                                    .putRequest(PutRequest.builder()
                                            .item(mapFromHistoricState(table, serializedCache, historicState))
                                            .build())
                                    .build())
                            .collect(Collectors.toList())))
                    .build());

            table.getHistoricStates().commit();
        }
    }

    private Map<String, AttributeValue> mapFromHistoricState(Table table, Map<State, AttributeValue> serializedCache,
                                                             Table.HistoricState historicState) {
        var item = new HashMap<String, AttributeValue>();
        item.put("TableId", AttributeValue.builder().s(table.getId().getId()).build());
        item.put("Timestamp", AttributeValue.builder().n(Long.toString(historicState.getTimestamp().toEpochMilli())).build());
        item.put("Previous", historicState.getPrevious().map(previous -> AttributeValue.builder().n(Long.toString(previous.toEpochMilli())).build()).orElse(null));
        item.put("GameId", AttributeValue.builder().s(table.getGame().getId().getId()).build());
        item.put("Expires", AttributeValue.builder().n(Long.toString(historicState.getExpires().getEpochSecond())).build());
        item.put("State", Optional.ofNullable(serializedCache.get(historicState.getState())).orElseGet(() -> mapFromState(table.getGame(), historicState.getState())));
        return item;
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
    public void update(Table table) throws TableConcurrentlyModifiedException {
        var serializeCache = Collections.<State, AttributeValue>emptyMap();

        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        var expressionAttributeNames = new HashMap<String, String>();
        expressionAttributeNames.put("#Status", "Status");

        var updateExpression = "SET Version=:Version" +
                ",#Status=:Status" +
                ",Updated=:Updated" +
                ",Expires=:Expires" +
                ",OwnerId=:OwnerId" +
                ",Players=:Players" +
                ",Options=:Options";

        expressionAttributeValues.put(":Version", AttributeValue.builder().n(
                // TODO Version can become required after backwards compatibility period
                Integer.toString(table.getVersion() != null ? table.getVersion() + 1 : FIRST_VERSION)).build());
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

        if (table.getCurrentState().isPresent()) {
            var currentState = table.getCurrentState().get();
            var state = currentState.getState();
            var serialized = mapFromState(table.getGame(), state);

            updateExpression += ",#State=:State,StateTimestamp=:StateTimestamp";
            expressionAttributeNames.put("#State", "State");

            expressionAttributeValues.put(":StateTimestamp", AttributeValue.builder().n(Long.toString(currentState.getTimestamp().toEpochMilli())).build());
            expressionAttributeValues.put(":State", serialized);

            if (currentState.getPrevious().isPresent()) {
                updateExpression += ",PreviousStateTimestamp=:PreviousStateTimestamp";
                expressionAttributeValues.put(":PreviousStateTimestamp", AttributeValue.builder().n(Long.toString(currentState.getPrevious().get().toEpochMilli())).build());
            }

            serializeCache = Collections.singletonMap(state, serialized);
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
            throw new TableConcurrentlyModifiedException(e);
        }

        addPendingHistoricStates(table, serializeCache);
        addLogEntries(table);

        updateLookupItems(table);
    }

    private void updateLookupItems(Table table) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("Id = :Id")
                .expressionAttributeValues(Collections.singletonMap(":Id", AttributeValue.builder()
                        .s(table.getId().getId())
                        .build()))
                .build());

        var lookupItemsBySortKey = response.items().stream()
                .filter(item -> !item.get("UserId").s().equals("Table-" + table.getId().getId())) // Filter out the main item
                .collect(Collectors.toMap(item -> item.get("UserId").s(), Function.identity()));

        var playersBySortKey = table.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .collect(Collectors.toMap(player -> "User-" + player.getUserId().orElseThrow().getId(), Function.identity()));

        var lookupItemsToDelete = lookupItemsBySortKey.entrySet().stream()
                .filter(item -> !playersBySortKey.containsKey(item.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        var lookupItemsToAdd = playersBySortKey.entrySet().stream()
                .filter(entry -> !lookupItemsBySortKey.containsKey(entry.getKey()))
                .map(entry -> mapLookupItem(table, entry.getValue()))
                .collect(Collectors.toList());

        if (!lookupItemsToAdd.isEmpty() || !lookupItemsToDelete.isEmpty()) {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Map.of(tableName, Stream.concat(
                            lookupItemsToDelete.stream().map(item -> WriteRequest.builder()
                                    .deleteRequest(DeleteRequest.builder()
                                            .key(item.entrySet().stream()
                                                    .filter(entry -> entry.getKey().equals("Id") || entry.getKey().equals("UserId"))
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                                            .build())
                                    .build()),
                            lookupItemsToAdd.stream().map(item -> WriteRequest.builder()
                                    .putRequest(PutRequest.builder()
                                            .item(item)
                                            .build())
                                    .build()))
                            .collect(Collectors.toList())))
                    .build());
        }

        playersBySortKey.entrySet().stream()
                .filter(entry -> lookupItemsBySortKey.containsKey(entry.getKey()))
                .forEach(entry -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                        .tableName(tableName)
                        .key(keyLookup(table.getId(), entry.getValue().getUserId().orElseThrow()))
                        .attributeUpdates(mapLookupItemUpdate(table, entry.getValue()))
                        .build()));
    }

    private Optional<Table> findOptionallyById(Table.Id id) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .consistentRead(true)
                .build());

        if (!response.hasItem()) {
            return Optional.empty();
        }
        return Optional.of(mapToTable(response.item()));
    }

    private Map<String, AttributeValue> mapLookupItem(Table table, Player player) {
        var map = new HashMap<>(keyLookup(table.getId(), player.getUserId().orElseThrow()));
        map.put("Status", AttributeValue.builder().s(player.getStatus().name()).build());
        map.put("Expires", AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build());
        return map;
    }

    private Map<String, AttributeValueUpdate> mapLookupItemUpdate(Table table, Player player) {
        var map = new HashMap<String, AttributeValueUpdate>();
        map.put("Status", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s(player.getStatus().name()).build()).build());
        map.put("Expires", AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().n(Long.toString(table.getExpires().getEpochSecond())).build()).build());
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

    private Table mapToTable(Map<String, AttributeValue> item) {
        var id = Table.Id.of(item.get("Id").s());

        var gameId = Game.Id.of(item.get("GameId").s());
        var game = games.get(gameId);

        Instant updated = Instant.ofEpochSecond(Long.parseLong(item.get("Updated").n()));
        return Table.builder()
                .id(id)
                // TODO Version can become required after backwards compatibility period
                .version(item.get("Version") != null ? Integer.valueOf(item.get("Version").n()) : null)
                .type(Table.Type.valueOf(item.get("Type").s()))
                .mode(Table.Mode.valueOf(item.get("Mode").s()))
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
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .ownerId(User.Id.of(item.get("OwnerId").s()))
                .players(item.get("Players").l().stream()
                        .map(this::mapToPlayer)
                        .collect(Collectors.toSet()))
                .currentState(Optional.ofNullable(item.get("State"))
                        .map(attributeValue -> DynamoDbJson.fromJson(attributeValue, game.getProvider().getStateDeserializer()::deserialize))
                        .map(state -> Table.CurrentState.of(state,
                                Optional.ofNullable(item.get("StateTimestamp"))
                                        .map(AttributeValue::n)
                                        .map(Long::parseLong)
                                        .map(Instant::ofEpochMilli)
                                        .orElse(updated),
                                Optional.ofNullable(item.get("PreviousStateTimestamp"))
                                        .map(AttributeValue::n)
                                        .map(Long::parseLong)
                                        .map(Instant::ofEpochMilli))))
                .historicStates(Table.HistoricStates.defer(timestamp -> getHistoricState(game, id, timestamp)))
                .log(new LazyLog(since -> findLogEntries(id, since)))
                .build();
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

    private Table.HistoricState mapToHistoricState(Map<String, AttributeValue> item, Game game) {
        var timestamp = Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n()));
        var previous = Optional.ofNullable(item.get("Previous"))
                .map(AttributeValue::n)
                .map(Long::parseLong)
                .map(Instant::ofEpochMilli);
        var expires = Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n()));
        var state = DynamoDbJson.fromJson(item.get("State"), game.getProvider().getStateDeserializer()::deserialize);
        return Table.HistoricState.of(timestamp, previous, state, expires);
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
                .created(Instant.ofEpochSecond(Long.parseLong(map.get("Created").n())))
                .updated(Instant.ofEpochSecond(Long.parseLong(map.get("Updated").n())))
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
        map.put("TurnLimit", player.getTurnLimit().map(turnLimit -> AttributeValue.builder().n(Long.toString(turnLimit.getEpochSecond())).build()).orElse(null));
        return AttributeValue.builder().m(map).build();
    }

    private Map<String, AttributeValue> key(Table.Id id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(id.getId()).build());
        key.put("UserId", AttributeValue.builder().s("Table-" + id.getId()).build());
        return key;
    }

    private Map<String, AttributeValue> keyLookup(Table.Id gameId, User.Id userId) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", AttributeValue.builder().s(gameId.getId()).build());
        key.put("UserId", AttributeValue.builder().s("User-" + userId.getId()).build());
        return key;
    }

    private Map<String, AttributeValue> mapFromLogEntry(Table.Id gameId, LogEntry logEntry) {
        var item = new HashMap<String, AttributeValue>();

        item.put("GameId", AttributeValue.builder().s(gameId.getId()).build());
        item.put("Timestamp", AttributeValue.builder().n(Long.toString(logEntry.getTimestamp().toEpochMilli())).build());
        item.put("UserId", logEntry.getUserId().map(userId -> AttributeValue.builder().s(userId.getId()).build()).orElse(null));
        item.put("PlayerId", AttributeValue.builder().s(logEntry.getPlayerId().getId()).build());
        item.put("Expires", AttributeValue.builder().n(Long.toString(logEntry.getExpires().getEpochSecond())).build());
        item.put("Type", AttributeValue.builder().s(logEntry.getType().name()).build());
        item.put("Parameters", AttributeValue.builder().l(logEntry.getParameters().stream()
                .map(param -> AttributeValue.builder().s(param).build())
                .collect(Collectors.toList()))
                .build());

        return item;
    }

    private LogEntry mapToLogEntry(Map<String, AttributeValue> item) {
        return LogEntry.builder()
                .timestamp(Instant.ofEpochMilli(Long.parseLong(item.get("Timestamp").n())))
                .expires(Instant.ofEpochSecond(Long.parseLong(item.get("Expires").n())))
                .playerId(item.containsKey("PlayerId") ? Player.Id.of(item.get("PlayerId").s()) : null)
                .userId(item.containsKey("UserId") ? User.Id.of(item.get("UserId").s()) : null)
                .type(LogEntry.Type.valueOf(item.get("Type").s()))
                .parameters(item.get("Parameters").l().stream()
                        .map(AttributeValue::s)
                        .collect(Collectors.toList()))
                .build();
    }

    private Stream<LogEntry> findLogEntries(Table.Id gameId, Instant since) {
        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":GameId", AttributeValue.builder().s(gameId.getId()).build());
        expressionAttributeValues.put(":Since", AttributeValue.builder().n(Long.toString(since.toEpochMilli())).build());

        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                .tableName(logTableName)
                .keyConditionExpression("GameId = :GameId AND #Timestamp > :Since")
                .expressionAttributeNames(Collections.singletonMap("#Timestamp", "Timestamp"))
                .expressionAttributeValues(expressionAttributeValues)
                .scanIndexForward(false)
                .build())
                .items().stream()
                .map(this::mapToLogEntry);
    }
}
