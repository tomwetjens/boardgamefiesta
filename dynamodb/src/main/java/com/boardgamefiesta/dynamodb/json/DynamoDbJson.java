package com.boardgamefiesta.dynamodb.json;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DynamoDbJson {

    public static AttributeValue toJson(Function<JsonBuilderFactory, JsonObject> serializer) {
        return ((DynamoDbJsonObject) serializer.apply(new DynamoDbJsonBuilderFactory())).getAttributeValue();
    }

    public static <T> T fromJson(AttributeValue attributeValue, Function<JsonObject, T> deserializer) {
        return deserializer.apply(new DynamoDbJsonObject(attributeValue));
    }

}
