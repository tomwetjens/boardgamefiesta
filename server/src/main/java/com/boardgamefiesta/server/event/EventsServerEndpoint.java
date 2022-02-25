/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.server.event;

import com.boardgamefiesta.domain.event.WebSocketConnection;
import com.boardgamefiesta.domain.event.WebSocketConnectionSender;
import com.boardgamefiesta.domain.event.WebSocketConnections;
import com.boardgamefiesta.domain.event.WebSocketServerEvent;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/events")
@Slf4j
public class EventsServerEndpoint implements WebSocketConnectionSender {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<User.Id, Set<Session>> USER_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Table.Id, Set<Session>> TABLE_SESSIONS = new ConcurrentHashMap<>();

    @Inject
    WebSocketConnections webSocketConnections;

    @Inject
    Users users;

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

            // TODO Remove when all clients have switched to WebSockets on API Gateway
            webSocketConnections.add(WebSocketConnection.createForUser(session.getId(), userId));
        });

        getTableId(session).ifPresent(tableId ->
                TABLE_SESSIONS.compute(tableId, (key, sessions) -> {
                    if (sessions == null) {
                        sessions = new TreeSet<>(Comparator.comparing(Session::getId));
                    }

                    sessions.add(session);

                    return sessions;
                }));
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

        // TODO Remove when all clients have switched to WebSockets on API Gateway
        webSocketConnections.remove(session.getId());

        getTableId(session).ifPresent(tableId ->
                TABLE_SESSIONS.computeIfPresent(tableId, (k, sessions) -> {
                    sessions.remove(session);

                    if (sessions.isEmpty()) {
                        // Clean up mapping once all sessions are closed
                        return null;
                    }

                    return sessions;
                }));
    }

    // TODO Remove when all clients have switched to WebSockets on API Gateway
    @OnMessage
    public void onMessage(Session session, String data) throws JsonProcessingException {
        var clientEvent = OBJECT_MAPPER.readValue(data, ClientEvent.class);

        switch (clientEvent.getType()) {
            case ACTIVE:
                CurrentUser.getUserId(session, users).ifPresent(userId ->
                        webSocketConnections.updateStatus(session.getId(), userId, Instant.now(), WebSocketConnection.Status.ACTIVE));
                break;
            case INACTIVE:
                CurrentUser.getUserId(session, users).ifPresent(userId ->
                        webSocketConnections.updateStatus(session.getId(), userId, Instant.now(), WebSocketConnection.Status.INACTIVE));
                break;
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        onClose(session);
    }

    @Override
    public void sendToTable(Table.Id tableId, WebSocketServerEvent event) {
        var data = toJSON(event);

        var sessions = TABLE_SESSIONS.get(tableId);
        if (sessions != null) {
            sessions.forEach(session -> session.getAsyncRemote().sendObject(data));
        }
    }

    @Override
    public void sendToUser(User.Id userId, WebSocketServerEvent event) {
        var sessions = USER_SESSIONS.get(userId);
        if (sessions != null) {
            var data = toJSON(event);

            sessions.forEach(session -> session.getAsyncRemote().sendObject(data));
        }
    }

    private static String toJSON(WebSocketServerEvent event) {
        try {
            return OBJECT_MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // TODO Wrap in better exception
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Table.Id> getTableId(Session session) {
        return Optional.empty();
    }
}
