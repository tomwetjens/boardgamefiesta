package com.boardgamefiesta.dynamodb.triggers;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.stream.Collectors;

public final class AttributeValues {

    public static Map<String, AttributeValue> toClientModel(Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> eventModel) {
        return eventModel.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, av -> toClientModel(av.getValue())));
    }

    public static software.amazon.awssdk.services.dynamodb.model.AttributeValue toClientModel(com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue attributeValue) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                .s(attributeValue.getS())
                .n(attributeValue.getN())
                .b(attributeValue.getB() != null ? SdkBytes.fromByteBuffer(attributeValue.getB()) : null)
                .ns(attributeValue.getNS())
                .ss(attributeValue.getSS())
                .bs(attributeValue.getBS() != null ? attributeValue.getBS().stream()
                        .map(SdkBytes::fromByteBuffer)
                        .collect(Collectors.toList()) : null)
                .bool(attributeValue.getBOOL())
                .l(attributeValue.getL() != null ? attributeValue.getL().stream()
                        .map(AttributeValues::toClientModel)
                        .collect(Collectors.toList()) : null)
                .m(attributeValue.getM() != null ? toClientModel(attributeValue.getM()) : null)
                .nul(attributeValue.getNULL())
                .build();
    }

}
