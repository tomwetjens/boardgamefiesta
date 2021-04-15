package com.boardgamefiesta.server.automa;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

@Slf4j
class SqsListener {

    public static final int VISIBILITY_TIMEOUT = 5;
    public static final int WAIT_TIME_SECONDS = 20;
    public static final int MAX_NUM_MESSAGES = 10;

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final Consumer<Message> handler;
    private final ManagedExecutor managedExecutor;

    private boolean running;

    SqsListener(@NonNull SqsClient sqsClient,
                @NonNull String queueUrl,
                @NonNull Consumer<Message> handler) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.handler = handler;
        this.managedExecutor = ManagedExecutor.builder().build();
    }

    private void listen() {
        try {
            log.debug("Entering receive messages loop");
            while (running) {
                try {
                    var response = receive();

                    if (response.hasMessages()) {
                        var latch = new CountDownLatch(response.messages().size());

                        response.messages().forEach(msg -> {
                            if (running) {
                                processMessage(msg)
                                        .exceptionally(e -> null) // ignore error, always count down
                                        .thenRun(latch::countDown);
                            } else {
                                latch.countDown();
                            }
                        });

                        if (running) {
                            latch.await(); // wait until all messages processed before receiving new messages
                        }
                    }
                } catch (SdkClientException e) {
                    log.error("I/O error when receiving messages from queue", e);
                } catch (SqsException e) {
                    log.error("Service error when receiving messages from queue", e);
                }
            }
        } catch (InterruptedException e) {
            log.warn("Receive messages loop interrupted");
        } catch (Exception e) {
            log.error("Exception in receive messages loop", e);
        } finally {
            log.debug("Exited receive messages loop");
        }
    }

    private ReceiveMessageResponse receive() {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(MAX_NUM_MESSAGES)
                .visibilityTimeout(VISIBILITY_TIMEOUT)
                .waitTimeSeconds(WAIT_TIME_SECONDS)
                .build());
    }

    private CompletableFuture<Void> processMessage(Message message) {
        log.debug("Received message: {}", message);
        return managedExecutor
                .runAsync(() -> handler.accept(message))
                .exceptionally(e -> {
                    log.error("Error processing message: " + message, e);
                    throw new RuntimeException("Exception in message handler", e);
                })
                .thenRun(() -> {
                    log.debug("Deleting message {} from queue", message.messageId());
                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build());
                });
    }

    public void start() {
        if (!running) {
            log.info("Starting receive loop");
            running = true;
            managedExecutor.submit(this::listen);
        }
    }

    public void stop() {
        if (running) {
            log.info("Stopping receive loop");
            running = false;
        }
    }

}
