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
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * @deprecated TODO Remove when all clients have switched to WebSockets on API Gateway
 */
@Deprecated
@ApplicationScoped
@ServerEndpoint("/tables/{tableId}/events")
@Slf4j
public class TableEventsEndpoint {

    @Inject
    EventsServerEndpoint eventsServerEndpoint;

    @OnOpen
    public void onOpen(Session session) {
        eventsServerEndpoint.addTableSession(session, getTableId(session));
    }

    @OnClose
    public void onClose(Session session) {
        eventsServerEndpoint.removeTableSession(session, getTableId(session));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        onClose(session);
    }

    private static Table.Id getTableId(Session session) {
        return Table.Id.of(session.getPathParameters().get("tableId"));
    }

}
