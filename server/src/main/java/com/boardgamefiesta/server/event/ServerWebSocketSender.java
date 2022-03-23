/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.websocket.WebSocketSender;
import com.boardgamefiesta.websocket.WebSocketServerEvent;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Slf4j
public class ServerWebSocketSender implements WebSocketSender {

    private static final Map<User.Id, Set<Session>> USER_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Table.Id, Set<Session>> TABLE_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void sendToTable(Table.Id tableId, WebSocketServerEvent event) {
        var sessions = TABLE_SESSIONS.get(tableId);
        if (sessions != null) {
            var data = event.toJSON();

            log.debug("Sending to {} sessions: {}", sessions.size(), data);

            sessions.forEach(session -> session.getAsyncRemote().sendObject(data));
        }
    }

    @Override
    public void sendToUser(User.Id userId, WebSocketServerEvent event) {
        var sessions = USER_SESSIONS.get(userId);
        if (sessions != null) {
            var data = event.toJSON();

            log.debug("Sending to {} sessions: {}", sessions.size(), data);

            sessions.forEach(session -> session.getAsyncRemote().sendObject(data));
        }
    }

    void registerUser(User.Id userId, Session session) {
        log.debug("Register WebSocket connection {} for user {} from: {}", session.getId(), userId.getId(), session.getUserProperties());

        USER_SESSIONS.compute(userId, (k, sessions) -> {
            if (sessions == null) {
                sessions = new TreeSet<>(Comparator.comparing(Session::getId));
            }

            sessions.add(session);

            return sessions;
        });
    }

    void unregisterUser(User.Id userId, Session session) {
        log.debug("Unregister WebSocket connection {} for user {} from: {}", session.getId(), userId.getId(), session.getUserProperties());

        USER_SESSIONS.computeIfPresent(userId, (k, sessions) -> {
            sessions.remove(session);

            if (sessions.isEmpty()) {
                // Clean up mapping once all sessions are closed
                return null;
            }

            return sessions;
        });
    }

    void registerTable(Table.Id tableId, Session session) {
        log.debug("Register WebSocket connection {} for table {} from: {}", session.getId(), tableId.getId(), session.getUserProperties());

        TABLE_SESSIONS.compute(tableId, (key, sessions) -> {
            if (sessions == null) {
                sessions = new TreeSet<>(Comparator.comparing(Session::getId));
            }

            sessions.add(session);

            return sessions;
        });
    }


    public void unregisterTable(Table.Id tableId, Session session) {
        log.debug("Unregister WebSocket connection {} for table {} from: {}", session.getId(), tableId.getId(), session.getUserProperties());

        TABLE_SESSIONS.computeIfPresent(tableId, (k, sessions) -> {
            sessions.remove(session);

            if (sessions.isEmpty()) {
                // Clean up mapping once all sessions are closed
                return null;
            }

            return sessions;
        });
    }
}
