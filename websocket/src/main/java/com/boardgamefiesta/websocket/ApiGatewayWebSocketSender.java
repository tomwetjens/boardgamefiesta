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

package com.boardgamefiesta.websocket;

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
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class ApiGatewayWebSocketSender implements WebSocketSender {

    private final WebSocketConnectionRepository webSocketConnections;

    private final ApiGatewayManagementApiClient apiGatewayManagementApiClient;

    @Inject
    public ApiGatewayWebSocketSender(@NonNull WebSocketConnectionRepository webSocketConnections,
                                     @NonNull @ConfigProperty(name = "bgf.ws.connections-endpoint") Optional<String> connectionsEndpoint) {
        this.webSocketConnections = webSocketConnections;

        var clientBuilder = ApiGatewayManagementApiClient.builder();

        connectionsEndpoint
                .map(URI::create)
                .ifPresent(clientBuilder::endpointOverride);

        apiGatewayManagementApiClient = clientBuilder.build();
    }

    @Override
    public void sendToTable(Table.Id tableId, WebSocketServerEvent event) {
        var sdkBytes = SdkBytes.fromString(event.toJSON(), StandardCharsets.UTF_8);

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
    public void sendToUser(User.Id userId, WebSocketServerEvent event) {
        var sdkBytes = SdkBytes.fromString(event.toJSON(), StandardCharsets.UTF_8);

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
