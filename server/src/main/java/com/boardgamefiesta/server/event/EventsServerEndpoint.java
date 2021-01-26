package com.boardgamefiesta.server.event;

import com.boardgamefiesta.server.domain.table.Player;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.table.Tables;
import com.boardgamefiesta.server.domain.user.Friend;
import com.boardgamefiesta.server.domain.user.User;
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
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/events")
@Slf4j
public class EventsServerEndpoint {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final Map<User.Id, Session> USER_SESSIONS = new ConcurrentHashMap<>();

    private final WebSocketConnections webSocketConnections;
    private final Tables tables;

    @Inject
    public EventsServerEndpoint(@NonNull WebSocketConnections webSocketConnections,
                                @NonNull Tables tables) {
        this.webSocketConnections = webSocketConnections;
        this.tables = tables;
    }

    @OnOpen
    public void onOpen(Session session) {
        currentUserId(session).ifPresent(userId -> {
            USER_SESSIONS.put(userId, session);
            webSocketConnections.add(WebSocketConnection.create(session.getId(), userId));
        });
    }

    @OnClose
    public void onClose(Session session) {
        currentUserId(session).ifPresent(USER_SESSIONS::remove);

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
        currentUserId(session).ifPresent(USER_SESSIONS::remove);

        webSocketConnections.remove(session.getId());
    }

    void accepted(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Accepted event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.ACCEPTED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void rejected(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Rejected event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.REJECTED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void started(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Started event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.STARTED, event.getTableId().getId(), null));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.ENDED, event.getTableId().getId(), null));
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.StateChanged event) {
        var table = tables.findById(event.getTableId(), false);
        // TODO Only notify other players who did not trigger the change
        notifyOtherPlayers(null, table, new Event(Event.EventType.STATE_CHANGED, event.getTableId().getId(), null));
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited event) {
        var table = tables.findById(event.getTableId(), true);
        notifyUser(event.getUserId(), new Event(Event.EventType.INVITED, event.getTableId().getId(), event.getUserId().getId()));
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.INVITED, table.getId().getId(), event.getUserId().getId()));
    }

    void uninvited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        var table = tables.findById(event.getTableId(), false);
        notifyUser(event.getUserId(), new Event(Event.EventType.UNINVITED, event.getTableId().getId(), event.getUserId().getId()));
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.UNINVITED, table.getId().getId(), event.getUserId().getId()));
    }

    void left(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Left event) {
        var table = tables.findById(event.getTableId(), false);
        notifyUser(event.getUserId(), new Event(Event.EventType.LEFT, event.getTableId().getId(), event.getUserId().getId()));
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.LEFT, event.getTableId().getId(), event.getUserId().getId()));
    }

    void abandoned(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Abandoned event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.ABANDONED, event.getTableId().getId(), null));
    }

    void proposedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ProposedToLeave event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.PROPOSED_TO_LEAVE, event.getTableId().getId(), event.getUserId().getId()));
    }

    void agreedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.AgreedToLeave event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.AGREED_TO_LEAVE, event.getTableId().getId(), event.getUserId().getId()));
    }

    void kicked(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.KICKED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void optionsChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.OptionsChanged event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.OPTIONS_CHANGED, event.getTableId().getId(), null));
    }

    void computerAdded(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ComputerAdded event) {
        var table = tables.findById(event.getTableId(), false);
        notifyOtherPlayers(null, table, new Event(Event.EventType.COMPUTER_ADDED, event.getTableId().getId(), null));
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
        Session session = USER_SESSIONS.get(userId);
        if (session != null) {
            session.getAsyncRemote().sendObject(JSONB.toJson(event));
        }
    }

    private Optional<User.Id> currentUserId(Session session) {
        return Optional.ofNullable(session.getUserPrincipal()).map(Principal::getName).map(User.Id::of);
    }

}
