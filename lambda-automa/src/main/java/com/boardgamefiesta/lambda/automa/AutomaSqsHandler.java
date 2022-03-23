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

package com.boardgamefiesta.lambda.automa;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.boardgamefiesta.domain.automa.AutomaExecutor;
import com.boardgamefiesta.domain.automa.AutomaRequest;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.lambda.sqs.SQSBatchResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named("automa")
@Slf4j
public class AutomaSqsHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    @Inject
    AutomaExecutor automaExecutor;

    @Override
    public SQSBatchResponse handleRequest(SQSEvent input, Context context) {
        log.info("Handling event: {} records", input.getRecords().size());
        return new SQSBatchResponse(input.getRecords().stream()
                .flatMap(message -> {
                    try {
                        handleMessage(message);
                        return Stream.empty();
                    } catch (Exception e) {
                        log.error("Error handling message {}: {}", message.getMessageId(), message.getBody(), e);
                        return Stream.of(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
                    }
                })
                .collect(Collectors.toList()));
    }

    private void handleMessage(SQSEvent.SQSMessage message) {
        log.info("Handling message: {}", message.getMessageId());

        var automaRequest = AutomaRequest.fromJSON(message.getBody());

        automaExecutor.execute(Table.Id.of(automaRequest.getTableId()),
                Player.Id.of(automaRequest.getPlayerId()));
    }
}
