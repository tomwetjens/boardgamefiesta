package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.domain.DomainService;
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
class AutomaScheduler implements DomainService {

    private final SqsClient sqsClient;
    private final String queueUrl;

    @Inject
    public AutomaScheduler(@NonNull SqsClient sqsClient,
                           @ConfigProperty(name = "bgf.sqs.queue-url") String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    void schedule(Table. Id tableId, Player.Id playerId) {
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
