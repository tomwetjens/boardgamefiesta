package com.wetjens.gwt.server.sqs;

import com.wetjens.gwt.server.domain.AutomaScheduler;
import com.wetjens.gwt.server.domain.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ApplicationScoped
@Slf4j
public class AutomaOperatorSqsQueue {

    static final String PRODUCER_INSTANCE_ID = UUID.randomUUID().toString();

    static final String PRODUCER_INSTANCE_ID_ATTRIBUTE_NAME = "ProducerInstanceId";
    static final MessageAttributeValue PRODUCER_INSTANCE_ID_ATTRIBUTE_VALUE = MessageAttributeValue.builder()
            .dataType("String")
            .stringValue(PRODUCER_INSTANCE_ID)
            .build();

    private static final Jsonb JSONB = JsonbBuilder.create();

    private final SqsClient sqsClient;
    private final SqsConfiguration sqsConfiguration;
    private final ExecutorService executorService;

    private Future<?> consumer;
    private boolean stopping;

    @Inject
    public AutomaOperatorSqsQueue(SqsClient sqsClient, SqsConfiguration sqsConfiguration) {
        this.sqsClient = sqsClient;
        this.sqsConfiguration = sqsConfiguration;

        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void start(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("Starting SQS consumer");

        consumer = executorService.submit(this::receiveMessages);
    }

    public void stop(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        log.info("Stopping SQS consumer");

        if (consumer != null) {
            stopping = true;
            consumer.cancel(true);
            consumer = null;
        }
    }

    //    @Override
    public void requestAutomatedAction(Table table) {
        ComputerActionRequest request = new ComputerActionRequest(table.getId().getId());

        log.debug("Sending request to SQS queue for game: {}", request);

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsConfiguration.getQueueUrl())
                .messageAttributes(Collections.singletonMap(PRODUCER_INSTANCE_ID_ATTRIBUTE_NAME, PRODUCER_INSTANCE_ID_ATTRIBUTE_VALUE))
                .messageBody(JSONB.toJson(request))
                .build());
    }

    private void receiveMessages() {
        log.info("Started receiving messages from SQS queue");

        while (!stopping) {
            log.debug("Receive messages from SQS queue: {}", sqsConfiguration.getQueueUrl());

            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(sqsConfiguration.getQueueUrl())
                    .messageAttributeNames(AutomaOperatorSqsQueue.PRODUCER_INSTANCE_ID_ATTRIBUTE_NAME)
                    .maxNumberOfMessages(100)
                    .visibilityTimeout(30000)
                    .waitTimeSeconds(20)
                    // TODO visibility timeout etc.
                    .build());

            if (response.hasMessages()) {
                response.messages().forEach(message -> {
                    log.debug("Received message: {}", message);
                    try {
                        processMessage(message);
                    } catch (RuntimeException e) {
                        log.error("Error processing message: {}", message, e);
                    }
                });
            }
        }

        log.info("Stopped receiving messages from SQS queue");
    }

    private void processMessage(Message message) {
        log.debug("Processing message: {}", message);

        if (message.hasAttributes()) {
            var producerInstanceIdAttribute = message.messageAttributes().get(AutomaOperatorSqsQueue.PRODUCER_INSTANCE_ID_ATTRIBUTE_NAME);
            if (producerInstanceIdAttribute != null && producerInstanceIdAttribute.equals(AutomaOperatorSqsQueue.PRODUCER_INSTANCE_ID_ATTRIBUTE_VALUE)) {
                log.debug("Ignoring message because it originated from same instance: {}", message);
                return;
            }
        }

        var request = JSONB.fromJson(message.body(), ComputerActionRequest.class);

//        AutomaOperator.Request event = new AutomaOperator.Request(Game.Id.of(request.getGameId()));
        AutomaScheduler.Request event = null;

        log.debug("Firing event: {}", event);
        CDI.current().getBeanManager().fireEvent(event);

        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(sqsConfiguration.getQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ComputerActionRequest {
        String gameId;
    }
}
