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

import com.boardgamefiesta.domain.event.WebSocketConnections;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
@Slf4j
public class APIGatewayWebSocketsSender implements WebSocketsSender {

    private final WebSocketConnections webSocketConnections;

    private final ApiGatewayManagementApiClient apiGatewayManagementApiClient;

    @Inject
    public APIGatewayWebSocketsSender(@NonNull WebSocketConnections webSocketConnections,
                                      @NonNull @ConfigProperty(name = "bgf.ws.connections-endpoint") String connectionsEndpoint) {
        this.webSocketConnections = webSocketConnections;

        apiGatewayManagementApiClient = ApiGatewayManagementApiClient.builder()
                .endpointOverride(URI.create(connectionsEndpoint))
                .build();
    }

    @Override
    public void sendToTable(Table.Id tableId, String data) {
        var sdkBytes = SdkBytes.fromString(data, StandardCharsets.UTF_8);

        webSocketConnections.findByTableId(tableId)
                .forEach(connectionId -> {
                    try {
                        apiGatewayManagementApiClient.postToConnection(PostToConnectionRequest.builder()
                                .connectionId(connectionId)
                                .data(sdkBytes)
                                .build());
                    } catch (GoneException e) {
                        // Ignore
                    } catch (SdkException e) {
                        log.debug("Could not send to WebSocket connection: {}", connectionId, e);
                    }
                });
    }

    @Override
    public void sendToUser(User.Id userId, String data) {
        var sdkBytes = SdkBytes.fromString(data, StandardCharsets.UTF_8);

        webSocketConnections.findByUserId(userId)
                .forEach(connectionId -> {
                    try {
                        apiGatewayManagementApiClient.postToConnection(PostToConnectionRequest.builder()
                                .connectionId(connectionId)
                                .data(sdkBytes)
                                .build());
                    } catch (GoneException e) {
                        // Ignore
                    } catch (SdkException e) {
                        log.debug("Could not send to WebSocket connection: {}", connectionId, e);
                    }
                });
    }

}
