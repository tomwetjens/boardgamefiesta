package com.wetjens.gwt.server.websockets;

import com.wetjens.gwt.server.domain.Table;
import com.wetjens.gwt.server.domain.Tables;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
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
public class EventsEndpoint {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final Map<User.Id, Session> SESSIONS = new ConcurrentHashMap<>();

    private final Tables tables;

    @Inject
    public EventsEndpoint(@NonNull Tables tables) {
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

    void accepted(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Accepted accepted) {
        notifyOtherPlayers(accepted.getUserId(), accepted.getTable(), new Event(EventType.ACCEPTED, accepted.getTable().getId().getId(), accepted.getUserId().getId()));
    }

    void rejected(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Rejected rejected) {
        notifyOtherPlayers(rejected.getUserId(), rejected.getTable(), new Event(EventType.REJECTED, rejected.getTable().getId().getId(), rejected.getUserId().getId()));
    }

    void started(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Started started) {
        notifyOtherPlayers(null, started.getTable(), new Event(EventType.STARTED, started.getTable().getId().getId(), null));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended ended) {
        notifyOtherPlayers(null, ended.getTable(), new Event(EventType.ENDED, ended.getTable().getId().getId(), null));
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.StateChanged stateChanged) {
        notifyOtherPlayers(null, stateChanged.getTable(), new Event(EventType.STATE_CHANGED, stateChanged.getTable().getId().getId(), null));
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited invited) {
        notifyUser(invited.getUserId(), new Event(EventType.INVITED, invited.getTable().getId().getId(), null));
        notifyOtherPlayers(invited.getUserId(), invited.getTable(), new Event(EventType.INVITED, invited.getTable().getId().getId(), invited.getUserId().getId()));
    }

    private void notifyOtherPlayers(User.Id currentUserId, Table table, Event event) {
        table.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .map(Player::getUserId)
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
