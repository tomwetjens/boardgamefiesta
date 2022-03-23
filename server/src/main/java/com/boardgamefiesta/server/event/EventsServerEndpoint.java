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
import com.boardgamefiesta.domain.user.Users;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Optional;

@ApplicationScoped
@ServerEndpoint("/events")
@Slf4j
public class EventsServerEndpoint {

    @Inject
    Users users;

    // Only enabled for local testing
    // TODO Remove this config property when server is no longer deployed on AWS (in favor of REST API Gateway and Lambda)
    @ConfigProperty(name = "bgf.ws.server.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    ServerWebSocketSender sender;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        if (!enabled) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "WebSockets not enabled"));
        }

        CurrentUser.getUserId(session, users).ifPresent(userId -> {
            sender.registerUser(userId, session);
        });

        getTableId(session).ifPresent(tableId ->
                sender.registerTable(tableId, session));
    }

    @OnClose
    public void onClose(Session session) {
        CurrentUser.getUserId(session, users).ifPresent(userId ->
                sender.unregisterUser(userId, session));

        getTableId(session).ifPresent(tableId ->
                sender.unregisterTable(tableId, session));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        onClose(session);
    }

    private static Optional<Table.Id> getTableId(Session session) {
        var param = session.getRequestParameterMap().get("table");
        return param != null && !param.isEmpty() ? Optional.of(Table.Id.of(param.get(0))) : Optional.empty();
    }
}
