package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
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
import javax.json.Json;
import javax.json.JsonObject;
import javax.transaction.Transactional;
import java.io.StringReader;

@ApplicationScoped
@Slf4j
@Transactional
class AutomaExecutor {

    public static final int MAX_RETRIES = 30;

    private final Tables tables;
    private final SqsListener listener;

    @Inject
    AutomaExecutor(@NonNull Tables tables,
                   @NonNull SqsClient sqsClient,
                   @ConfigProperty(name = "bgf.sqs.enabled") boolean enabled,
                   @ConfigProperty(name = "bgf.sqs.queue-url") String queueUrl) {
        this.tables = tables;

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
        var body = message.body();
        var jsonObject = parseJsonString(body);

        execute(Table.Id.of(jsonObject.getString("tableId")),
                Player.Id.of(jsonObject.getString("playerId")));
    }

    private JsonObject parseJsonString(String str) {
        return Json.createReader(new StringReader(str)).readObject();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void execute(Table.Id tableId, Player.Id playerId) {
        try {
            var retries = 0;
            do {
                log.debug("Executing for table {} and player {}", tableId.getId(), playerId.getId());

                var table = tables.findById(tableId)
                        .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId.getId()));

                if (table.getStatus() != Table.Status.STARTED) {
                    return;
                }

                var player = table.getPlayerById(playerId)
                        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId.getId()));

                if (player.getType() != Player.Type.COMPUTER) {
                    return;
                }

                if (!table.getCurrentPlayers().contains(player)) {
                    return;
                }

                table.executeAutoma(player);

                try {
                    tables.update(table);
                    return;
                } catch (Tables.ConcurrentModificationException e) {
                    if (retries >= MAX_RETRIES) {
                        throw new RuntimeException("Executor failed after " + retries + " retries. Table id " + table.getId().getId() + ", version " + table.getVersion(), e);
                    }

                    retries++;
                }
            } while (true);
        } catch (RuntimeException e) {
            log.error("Error executing request", e);
            throw e;
        }
    }

}
