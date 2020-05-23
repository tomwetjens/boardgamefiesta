package com.tomsboardgames.server.event;

import com.tomsboardgames.server.domain.Player;
import com.tomsboardgames.server.domain.Table;
import com.tomsboardgames.server.domain.Tables;
import com.tomsboardgames.server.domain.User;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/events")
@Slf4j
public class EventsServerEndpoint {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final Map<User.Id, Session> SESSIONS = new ConcurrentHashMap<>();

    private final Tables tables;

    @Inject
    public EventsServerEndpoint(@NonNull Tables tables) {
        this.tables = tables;
    }

    @OnOpen
    public void onOpen(Session session) {
        User.Id currentUserId = currentUserId(session);
        SESSIONS.put(currentUserId, session);
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(currentUserId(session));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        SESSIONS.remove(currentUserId(session));
    }

    void accepted(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Accepted event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.ACCEPTED, table.getId().getId(), event.getUserId().getId()));
    }

    void rejected(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Rejected event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.REJECTED, table.getId().getId(), event.getUserId().getId()));
    }

    void started(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Started event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.STARTED, table.getId().getId(), null));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.ENDED, table.getId().getId(), null));
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.StateChanged event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.STATE_CHANGED, table.getId().getId(), null));
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited event) {
        var table = tables.findById(event.getTableId());
        notifyUser(event.getUserId(), new Event(Event.EventType.INVITED, table.getId().getId(), null));
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.INVITED, table.getId().getId(), event.getUserId().getId()));
    }

    void uninvited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        var table = tables.findById(event.getTableId());
        notifyUser(event.getUserId(), new Event(Event.EventType.UNINVITED, table.getId().getId(), null));
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.UNINVITED, table.getId().getId(), event.getUserId().getId()));
    }

    void left(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Left event) {
        var table = tables.findById(event.getTableId());
        notifyUser(event.getUserId(), new Event(Event.EventType.LEFT, event.getTableId().getId(), null));
        notifyOtherPlayers(event.getUserId(), table, new Event(Event.EventType.LEFT, event.getTableId().getId(), event.getUserId().getId()));
    }

    void abandoned(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Abandoned event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.ABANDONED, event.getTableId().getId(), null));
    }

    void proposedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ProposedToLeave event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.PROPOSED_TO_LEAVE, event.getTableId().getId(), null));
    }

    void agreedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.AgreedToLeave event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.AGREED_TO_LEAVE, event.getTableId().getId(), null));
    }

    void kicked(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.KICKED, event.getTableId().getId(), null));
    }

    void optionsChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.OptionsChanged event) {
        var table = tables.findById(event.getTableId());
        notifyOtherPlayers(null, table, new Event(Event.EventType.OPTIONS_CHANGED, event.getTableId().getId(), null));
    }

    private void notifyOtherPlayers(User.Id currentUserId, Table table, Event event) {
        table.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .flatMap(player -> player.getUserId().stream())
                .filter(userId -> !userId.equals(currentUserId))
                .forEach(userId -> notifyUser(userId, event));
    }

    private void notifyUser(User.Id userId, Event event) {
        Session session = SESSIONS.get(userId);
        if (session != null) {
            session.getAsyncRemote().sendObject(JSONB.toJson(event));
        }
    }

    private User.Id currentUserId(Session session) {
        return User.Id.of(session.getUserPrincipal().getName());
    }

}
