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

package com.boardgamefiesta.lambda.ws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketResponse;
import com.boardgamefiesta.websocket.WebSocketConnection;
import com.boardgamefiesta.websocket.WebSocketConnectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;

@Named("DefaultHandler")
@Slf4j
public class DefaultHandler implements RequestHandler<APIGatewayV2WebSocketEvent, APIGatewayV2WebSocketResponse> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    WebSocketConnectionRepository webSocketConnections;

    @SneakyThrows
    @Override
    public APIGatewayV2WebSocketResponse handleRequest(APIGatewayV2WebSocketEvent input, Context context) {
        var clientEvent = OBJECT_MAPPER.readValue(input.getBody(), ClientEvent.class);

        var connectionId = input.getRequestContext().getConnectionId();

        webSocketConnections.findByConnectionId(connectionId)
                .ifPresent(webSocketConnection -> {
                    switch (clientEvent.getType()) {
                        case ACTIVE:
                            webSocketConnections.updateStatus(connectionId, webSocketConnection.getUserId(), Instant.now(), WebSocketConnection.Status.ACTIVE);
                            break;
                        case INACTIVE:
                            webSocketConnections.updateStatus(connectionId, webSocketConnection.getUserId(), Instant.now(), WebSocketConnection.Status.INACTIVE);
                            break;
                    }
                });

        var response = new APIGatewayV2WebSocketResponse();
        response.setStatusCode(200);

        return response;
    }
}
