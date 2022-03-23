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
    public static final int MAX_NUM_MESSAGES = 10; // max 10

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
        this.managedExecutor = ManagedExecutor.builder()
                .maxQueued(-1)
                .maxAsync(1)
                .maxAsync(MAX_NUM_MESSAGES + 1) // including listener loop itself
                .build();
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
