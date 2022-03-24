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

package com.boardgamefiesta.domain.automa;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
@Slf4j
class AutomaScheduler {

    private final SqsClient sqsClient;
    private final String queueUrl;

    @Inject
    public AutomaScheduler(@NonNull SqsClient sqsClient,
                           @ConfigProperty(name = "bgf.sqs.queue-url") Optional<String> queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl.orElse(null);
    }

    public void schedule(Table.Id tableId, Player.Id playerId) {
        if (queueUrl == null) {
            throw new IllegalStateException("SQS queue URL not configured!");
        }

        // Send to external queue so it is persisted and can be picked by a Lambda
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(new AutomaRequest(tableId.getId(), playerId.getId()).toJSON())
                .build());
    }

}
