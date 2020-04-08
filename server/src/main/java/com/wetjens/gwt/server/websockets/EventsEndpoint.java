package com.wetjens.gwt.server.websockets;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/events")
@Slf4j
public class EventsEndpoint {

    public static final Jsonb JSONB = JsonbBuilder.create();
    private final Map<User.Id, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        log.info("onOpen: user={}", session.getUserPrincipal().getName());

        User.Id currentUserId = currentUserId(session);
        sessions.put(currentUserId, session);
    }

    public User.Id currentUserId(Session session) {
        return User.Id.of(session.getUserPrincipal().getName());
    }

    @OnClose
    public void onClose(Session session) {
        log.info("onClose: user={}", session.getUserPrincipal().getName());

        sessions.remove(currentUserId(session));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.info("onError: user={} throwable={}", session.getUserPrincipal().getName(), throwable);

        sessions.remove(currentUserId(session));
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        log.info("onMessage: user={} message={}", session.getUserPrincipal().getName(), message);

        Event event = JSONB.fromJson(message, Event.class);

        log.info("onMessage: event={}", event);
    }

    void accepted(@Observes Game.Accepted accepted) {
        notifyOtherPlayers(accepted.getUserId(), accepted.getGame(), new Event(EventType.ACCEPTED, accepted.getGame().getId().getId(), accepted.getUserId().getId()));
    }

    private void notifyOtherPlayers(User.Id exclude, Game game, Event event) {
        game.getPlayers().stream()
                .map(Player::getUserId)
                .filter(userId -> !userId.equals(exclude))
                .filter(sessions::containsKey)
                .map(sessions::get)
                .forEach(session -> session.getAsyncRemote().sendObject(JSONB.toJson(event)));
    }

    void rejected(@Observes Game.Rejected rejected) {
        notifyOtherPlayers(rejected.getUserId(), rejected.getGame(), new Event(EventType.REJECTED, rejected.getGame().getId().getId(), rejected.getUserId().getId()));
    }

    void started(@Observes Game.Started started) {
        notifyOtherPlayers(null, started.getGame(), new Event(EventType.STARTED, started.getGame().getId().getId(), null));
    }

    void ended(@Observes Game.Ended ended) {
        notifyOtherPlayers(null, ended.getGame(), new Event(EventType.ENDED, ended.getGame().getId().getId(), null));
    }

    void stateChanged(@Observes Game.StateChanged stateChanged) {
        notifyOtherPlayers(null, stateChanged.getGame(), new Event(EventType.STATE_CHANGED, stateChanged.getGame().getId().getId(), null));
    }

    void invited(@Observes Game.Invited invited) {
        Session session = sessions.get(invited.getUserId());

        if (session != null) {
            session.getAsyncRemote().sendObject("INVITED");
        }
    }
}
