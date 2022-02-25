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

import com.boardgamefiesta.domain.table.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@ServerEndpoint("/tables/{tableId}/events")
@Slf4j
public class TableEventsEndpoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<Table.Id, Set<Session>> SESSIONS = new ConcurrentHashMap<>();

    @Inject
    WebSocketsSender webSocketsSender;

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

    private void broadcast(Table.Id tableId, Event event) {
        var data = toJSON(event);

        var sessions = SESSIONS.get(tableId);
        if (sessions != null) {
            sessions.forEach(session -> session.getAsyncRemote().sendObject(data));
        }

        webSocketsSender.sendToTable(tableId, data);
    }

    private static String toJSON(Event event) {
        try {
            return OBJECT_MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // TODO Wrap in better exception
            throw new UncheckedIOException(e);
        }
    }

    private static Table.Id getTableId(Session session) {
        return Table.Id.of(session.getPathParameters().get("tableId"));
    }

}
