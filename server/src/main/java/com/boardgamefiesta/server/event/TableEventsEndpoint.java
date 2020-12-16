package com.boardgamefiesta.server.event;

import com.boardgamefiesta.server.domain.table.Table;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/tables/{tableId}/events")
@Slf4j
public class TableEventsEndpoint {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final Map<Table.Id, Set<Session>> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        var tableId = getTableId(session);

        SESSIONS.compute(tableId, (key, sessions) -> {
            if (sessions == null) {
                sessions = new TreeSet<>(Comparator.comparing(Session::getId));
            }

            sessions.add(session);

            return sessions;
        });
    }

    @OnClose
    public void onClose(Session session) {
        var tableId = getTableId(session);

        SESSIONS.computeIfPresent(tableId, (k, sessions) -> {
            sessions.remove(session);

            if (sessions.isEmpty()) {
                // Clean up mapping once all sessions are closed
                return null;
            }

            return sessions;
        });
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        onClose(session);
    }

    void accepted(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Accepted event) {
        broadcast(event.getTableId(), new Event(Event.EventType.ACCEPTED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void rejected(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Rejected event) {
        broadcast(event.getTableId(), new Event(Event.EventType.REJECTED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void started(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Started event) {
        broadcast(event.getTableId(), new Event(Event.EventType.STARTED, event.getTableId().getId(), null));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        broadcast(event.getTableId(), new Event(Event.EventType.ENDED, event.getTableId().getId(), null));
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.StateChanged event) {
        broadcast(event.getTableId(), new Event(Event.EventType.STATE_CHANGED, event.getTableId().getId(), null));
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited event) {
        broadcast(event.getTableId(), new Event(Event.EventType.INVITED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void uninvited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        broadcast(event.getTableId(), new Event(Event.EventType.UNINVITED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void joined(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Joined event) {
        broadcast(event.getTableId(), new Event(Event.EventType.JOINED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void visibilityChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.VisibilityChanged event) {
        broadcast(event.getTableId(), new Event(Event.EventType.VISIBILITY_CHANGED, event.getTableId().getId(), null));
    }

    void left(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Left event) {
        broadcast(event.getTableId(), new Event(Event.EventType.LEFT, event.getTableId().getId(), event.getUserId().getId()));
    }

    void abandoned(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Abandoned event) {
        broadcast(event.getTableId(), new Event(Event.EventType.ABANDONED, event.getTableId().getId(), null));
    }

    void proposedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ProposedToLeave event) {
        broadcast(event.getTableId(), new Event(Event.EventType.PROPOSED_TO_LEAVE, event.getTableId().getId(), event.getUserId().getId()));
    }

    void agreedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.AgreedToLeave event) {
        broadcast(event.getTableId(), new Event(Event.EventType.AGREED_TO_LEAVE, event.getTableId().getId(), event.getUserId().getId()));
    }

    void kicked(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        broadcast(event.getTableId(), new Event(Event.EventType.KICKED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void optionsChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.OptionsChanged event) {
        broadcast(event.getTableId(), new Event(Event.EventType.OPTIONS_CHANGED, event.getTableId().getId(), null));
    }

    void computerAdded(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ComputerAdded event) {
        broadcast(event.getTableId(), new Event(Event.EventType.COMPUTER_ADDED, event.getTableId().getId(), null));
    }

    private static void broadcast(Table.Id tableId, Event event) {
        var sessions = SESSIONS.get(tableId);

        if (sessions != null) {
            var data = JSONB.toJson(event);

            sessions.forEach(session -> session.getAsyncRemote().sendObject(data));
        }
    }

    private static Table.Id getTableId(Session session) {
        return Table.Id.of(session.getPathParameters().get("tableId"));
    }

}
