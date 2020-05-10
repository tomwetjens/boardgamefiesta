package com.wetjens.gwt.server.websockets;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
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

    private final Games games;

    @Inject
    public EventsEndpoint(@NonNull Games games) {
        this.games = games;
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

    void accepted(@Observes(during = TransactionPhase.AFTER_SUCCESS) Game.Accepted accepted) {
        notifyOtherPlayers(accepted.getUserId(), accepted.getGame(), new Event(EventType.ACCEPTED, accepted.getGame().getId().getId(), accepted.getUserId().getId()));
    }

    void rejected(@Observes(during = TransactionPhase.AFTER_SUCCESS) Game.Rejected rejected) {
        notifyOtherPlayers(rejected.getUserId(), rejected.getGame(), new Event(EventType.REJECTED, rejected.getGame().getId().getId(), rejected.getUserId().getId()));
    }

    void started(@Observes(during = TransactionPhase.AFTER_SUCCESS) Game.Started started) {
        notifyOtherPlayers(null, started.getGame(), new Event(EventType.STARTED, started.getGame().getId().getId(), null));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Game.Ended ended) {
        notifyOtherPlayers(null, ended.getGame(), new Event(EventType.ENDED, ended.getGame().getId().getId(), null));
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Game.StateChanged stateChanged) {
        notifyOtherPlayers(null, stateChanged.getGame(), new Event(EventType.STATE_CHANGED, stateChanged.getGame().getId().getId(), null));
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Game.Invited invited) {
        notifyUser(invited.getUserId(), new Event(EventType.INVITED, invited.getGame().getId().getId(), null));
        notifyOtherPlayers(invited.getUserId(), invited.getGame(), new Event(EventType.INVITED, invited.getGame().getId().getId(), invited.getUserId().getId()));
    }

    private void notifyOtherPlayers(User.Id currentUserId, Game game, Event event) {
        game.getPlayers().stream()
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
