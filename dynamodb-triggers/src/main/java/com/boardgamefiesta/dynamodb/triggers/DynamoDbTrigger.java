package com.boardgamefiesta.dynamodb.triggers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@Slf4j
abstract class DynamoDbTrigger implements RequestStreamHandler {

    private static final PojoSerializer<DynamodbEvent> SERIALIZER = LambdaEventSerializers.serializerFor(DynamodbEvent.class, DynamodbEvent.class.getClassLoader());

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try {
            var event = SERIALIZER.fromJson(input);

            handleRequest(event);
        } catch (RuntimeException e) {
            log.error("Error handling request", e);
            throw e;
        }
    }

    private void handleRequest(DynamodbEvent event) {
        event.getRecords().forEach(record -> {
            switch (OperationType.fromValue(record.getEventName())) {
                case INSERT:
                    log.info("INSERT: {}", record.getDynamodb().getNewImage());
                    handleInsert(AttributeValues.toClientModel(record.getDynamodb().getNewImage()));
                    break;
                case MODIFY:
                    log.info("MODIFY: {}", record.getDynamodb().getNewImage());
                    handleModify(AttributeValues.toClientModel(record.getDynamodb().getNewImage()));
                    break;
                case REMOVE:
                    log.info("REMOVE: {}", record.getDynamodb().getKeys());
                    handleRemove(AttributeValues.toClientModel(record.getDynamodb().getKeys()));
                    break;
            }
        });
    }

    abstract void handleInsert(Map<String, AttributeValue> item);

    abstract void handleModify(Map<String, AttributeValue> item);

    abstract void handleRemove(Map<String, AttributeValue> key);
}
