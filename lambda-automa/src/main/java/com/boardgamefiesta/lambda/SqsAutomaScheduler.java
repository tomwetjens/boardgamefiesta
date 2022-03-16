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

package com.boardgamefiesta.lambda;

import com.boardgamefiesta.domain.table.AutomaScheduler;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringWriter;

@ApplicationScoped
@Slf4j
class SqsAutomaScheduler implements AutomaScheduler {

    private final SqsClient sqsClient;
    private final String queueUrl;

    @Inject
    public SqsAutomaScheduler(@NonNull SqsClient sqsClient,
                              @ConfigProperty(name = "bgf.sqs.queue-url") String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void schedule(Table.Id tableId, Player.Id playerId) {
        // Send to external queue so it is persisted and can be picked by any worker
        var message = Json.createObjectBuilder()
                .add("tableId", tableId.getId())
                .add("playerId", playerId.getId())
                .build();

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(toJsonString(message))
                .delaySeconds(2) // simulate computer thinking
                .build());
    }

    private String toJsonString(JsonObject jsonObject) {
        var writer = new StringWriter();
        Json.createWriter(writer).writeObject(jsonObject);
        return writer.toString();
    }

}
