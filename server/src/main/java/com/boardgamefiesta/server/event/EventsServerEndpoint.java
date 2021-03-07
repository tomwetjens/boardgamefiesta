package com.boardgamefiesta.server.event;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.event.domain.WebSocketConnection;
import com.boardgamefiesta.server.event.domain.WebSocketConnections;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/events")
@Slf4j
public class EventsServerEndpoint {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final Map<User.Id, Set<Session>> USER_SESSIONS = new ConcurrentHashMap<>();

    private final WebSocketConnections webSocketConnections;
    private final Users users;
    private final Tables tables;

    @Inject
    public EventsServerEndpoint(@NonNull WebSocketConnections webSocketConnections,
                                @NonNull Users users,
                                @NonNull Tables tables) {
        this.webSocketConnections = webSocketConnections;
        this.users = users;
        this.tables = tables;
    }

    @OnOpen
    public void onOpen(Session session) {
        CurrentUser.getUserId(session, users).ifPresent(userId -> {
            USER_SESSIONS.compute(userId, (k, sessions) -> {
                if (sessions == null) {
                    sessions = new TreeSet<>(Comparator.comparing(Session::getId));
                }

                sessions.add(session);

                return sessions;
            });

            webSocketConnections.add(WebSocketConnection.create(session.getId(), userId));
        });
    }

    @OnClose
    public void onClose(Session session) {
        CurrentUser.getUserId(session, users).ifPresent(userId ->
                USER_SESSIONS.computeIfPresent(userId, (k, sessions) -> {
                    sessions.remove(session);

                    if (sessions.isEmpty()) {
                        // Clean up mapping once all sessions are closed
                        return null;
                    }

                    return sessions;
                }));

        webSocketConnections.remove(session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String data) {
        var clientEvent = JSONB.fromJson(data, ClientEvent.class);

        switch (clientEvent.getType()) {
            case ACTIVE:
                webSocketConnections.updateStatus(session.getId(), Instant.now(), WebSocketConnection.Status.ACTIVE);
                break;
            case INACTIVE:
                webSocketConnections.updateStatus(session.getId(), Instant.now(), WebSocketConnection.Status.INACTIVE);
                break;
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        onClose(session);
    }

    void accepted(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Accepted event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.ACCEPTED, event.getTableId().getId(), event.getUserId().getId())));
    }

    void rejected(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Rejected event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.REJECTED, event.getTableId().getId(), event.getUserId().getId())));
    }

    void started(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Started event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new Event(Event.EventType.STARTED, event.getTableId().getId(), null)));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new Event(Event.EventType.ENDED, event.getTableId().getId(), null)));
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.StateChanged event) {
        // TODO Only notify other players who did not trigger the change
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new Event(Event.EventType.STATE_CHANGED, event.getTableId().getId(), null)));
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited event) {
        tables.findById(event.getTableId()).ifPresent(table -> {
            notifyUser(event.getUserId(), new Event(Event.EventType.INVITED, event.getTableId().getId(), event.getUserId().getId()));
            notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.INVITED, table.getId().getId(), event.getUserId().getId()));
        });
    }

    void uninvited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        tables.findById(event.getTableId()).ifPresent(table -> {
            notifyUser(event.getUserId(), new Event(Event.EventType.UNINVITED, event.getTableId().getId(), event.getUserId().getId()));
            notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.UNINVITED, table.getId().getId(), event.getUserId().getId()));
        });
    }

    void left(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Left event) {
        tables.findById(event.getTableId()).ifPresent(table -> {
            notifyUser(event.getUserId(), new Event(Event.EventType.LEFT, event.getTableId().getId(), event.getUserId().getId()));
            notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.LEFT, event.getTableId().getId(), event.getUserId().getId()));
        });
    }

    void abandoned(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Abandoned event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new Event(Event.EventType.ABANDONED, event.getTableId().getId(), null)));
    }

    void kicked(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new Event(Event.EventType.KICKED, event.getTableId().getId(), event.getUserId().getId())));
    }

    void optionsChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.OptionsChanged event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new Event(Event.EventType.OPTIONS_CHANGED, event.getTableId().getId(), null)));
    }

    void computerAdded(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ComputerAdded event) {
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new Event(Event.EventType.COMPUTER_ADDED, event.getTableId().getId(), null)));
    }

    void addedAsFriend(@Observes(during = TransactionPhase.AFTER_SUCCESS) Friend.Started event) {
        notifyUser(event.getId().getOtherUserId(), new Event(Event.EventType.ADDED_AS_FRIEND, null, event.getId().getUserId().getId()));
    }

    private void notifyOtherPlayers(User.Id currentUserId, Table table, Event event) {
        table.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .flatMap(player -> player.getUserId().stream())
                .filter(userId -> !userId.equals(currentUserId))
                .forEach(userId -> notifyUser(userId, event));
    }

    private void notifyUser(User.Id userId, Event event) {
        var sessions = USER_SESSIONS.get(userId);
        if (sessions != null) {
            sessions.forEach(session -> session.getAsyncRemote().sendObject(JSONB.toJson(event)));
        }
    }

}
