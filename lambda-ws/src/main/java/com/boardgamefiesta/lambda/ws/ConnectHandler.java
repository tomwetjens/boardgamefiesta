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
import com.boardgamefiesta.domain.event.WebSocketConnection;
import com.boardgamefiesta.domain.event.WebSocketConnections;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.lambda.ws.oidc.OidcAuthenticationException;
import com.boardgamefiesta.lambda.ws.oidc.OidcAuthenticator;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("ConnectHandler")
@Slf4j
public class ConnectHandler implements RequestHandler<APIGatewayV2WebSocketEvent, APIGatewayV2WebSocketResponse> {

    @Inject
    OidcAuthenticator oidcAuthenticator;

    @Inject
    Users users;

    @Inject
    WebSocketConnections webSocketConnections;

    @Override
    public APIGatewayV2WebSocketResponse handleRequest(APIGatewayV2WebSocketEvent input, Context context) {
        var queryParams = input.getQueryStringParameters();

        var response = new APIGatewayV2WebSocketResponse();

        var token = queryParams.get("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(401);
            return response;
        }

        var tableId = queryParams.containsKey("table") ? Table.Id.of(queryParams.get("table")) : null;

        try {
            var principal = oidcAuthenticator.authenticate(token);

            var userId = users.findIdByCognitoUsername(principal.getName())
                    .orElseThrow(() -> new OidcAuthenticationException("User not found"));

            var connectionId = input.getRequestContext().getConnectionId();
            log.info("Adding WebSocket connection: {} for user {}", connectionId, userId);
            webSocketConnections.add(WebSocketConnection.createForTable(connectionId, userId, tableId));

            response.setStatusCode(200);
            return response;
        } catch (OidcAuthenticationException e) {
            log.error("Authentication error", e);
            response.setStatusCode(401);
            return response;
        }
    }
}
