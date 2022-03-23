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

package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.domain.automa.AutomaExecutor;
import com.boardgamefiesta.domain.automa.AutomaRequest;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
@Slf4j
@Transactional
class AutomaSqsHandler {

    private final SqsListener listener;
    private final AutomaExecutor automaExecutor;

    @Inject
    AutomaSqsHandler(@NonNull AutomaExecutor automaExecutor,
                     @NonNull SqsClient sqsClient,
                     @ConfigProperty(name = "bgf.sqs.listen", defaultValue = "false") boolean enabled,
                     @ConfigProperty(name = "bgf.sqs.queue-url") String queueUrl) {
        this.automaExecutor = automaExecutor;

        if (enabled) {
            listener = new SqsListener(sqsClient, queueUrl, this::processMessage);
        } else {
            listener = null;
        }
    }

    public void init(@Observes StartupEvent event) {
        if (listener != null) {
            listener.start();
        }
    }

    public void destroy(@Observes ShutdownEvent event) {
        if (listener != null) {
            listener.stop();
        }
    }

    private void processMessage(Message message) {
        var automaRequest = AutomaRequest.fromJSON(message.body());

        log.debug("Processing automa request: {}", automaRequest);

        automaExecutor.execute(Table.Id.of(automaRequest.getTableId()),
                Player.Id.of(automaRequest.getPlayerId()));
    }

}
