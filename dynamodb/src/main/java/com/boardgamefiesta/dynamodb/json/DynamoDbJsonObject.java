package com.boardgamefiesta.dynamodb.json;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class DynamoDbJsonObject extends DynamoDbJsonValue implements JsonObject {

    @Getter(value = AccessLevel.PACKAGE)
    private final AttributeValue attributeValue;

    @Override
    public JsonArray getJsonArray(String key) {
        var value = attributeValue.m().get(key);
        if (value == null || Boolean.TRUE.equals(value.nul())) {
            return null;
        }
        if (!value.hasL()) {
            // This can happen when an empty "L" is converted by DynamodbAttributeValueTransformer.toAttributeValueMapV2
            return EMPTY_JSON_ARRAY;
        }
        return new DynamoDbJsonArray(value);
    }

    @Override
    public JsonObject getJsonObject(String key) {
        var value = attributeValue.m().get(key);
        if (value == null || Boolean.TRUE.equals(value.nul())) {
            return null;
        }
        if (!value.hasM()) {
            return EMPTY_JSON_OBJECT;
        }
        return new DynamoDbJsonObject(value);
    }

    @Override
    public JsonNumber getJsonNumber(String key) {
        JsonValue jsonValue = DynamoDbJsonValue.of(attributeValue.m().get(key));
        if (jsonValue == JsonValue.NULL) {
            throw new NullPointerException();
        }
        return (JsonNumber) jsonValue;
    }

    @Override
    public JsonString getJsonString(String key) {
        JsonValue jsonValue = DynamoDbJsonValue.of(attributeValue.m().get(key));
        if (jsonValue == JsonValue.NULL) {
            throw new NullPointerException();
        }
        return (JsonString) jsonValue;
    }

    @Override
    public String getString(String key) {
        var value = this.attributeValue.m().get(key);
        return value != null ? value.s() : null;
    }

    @Override
    public String getString(String key, String defaultValue) {
        var value = attributeValue.m().get(key);
        return value != null ? value.s() : defaultValue;
    }

    @Override
    public int getInt(String key) {
        return Integer.parseInt(attributeValue.m().get(key).n());
    }

    @Override
    public int getInt(String key, int defaultValue) {
        var value = attributeValue.m().get(key);
        return value != null ? Integer.parseInt(value.n()) : defaultValue;
    }

    @Override
    public boolean getBoolean(String key) {
        return attributeValue.m().get(key).bool();
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        var value = attributeValue.m().get(key);
        return value != null ? value.bool() : defaultValue;
    }

    @Override
    public boolean isNull(String key) {
        return attributeValue.m().get(key).nul();
    }

    @Override
    public int size() {
        return attributeValue.m().size();
    }

    @Override
    public boolean isEmpty() {
        return attributeValue.m().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return attributeValue.m().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return attributeValue.m().containsValue(((DynamoDbJsonValue) value).getAttributeValue());
    }

    @Override
    public JsonValue get(Object key) {
        var attributeValue = this.attributeValue.m().get(key);
        return DynamoDbJsonValue.of(attributeValue);
    }

    @Override
    public JsonValue put(String key, JsonValue value) {
        throw readOnly();
    }

    private static UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("JsonObjects from DynamoDB are read only views on AttributeValue");
    }

    @Override
    public JsonValue remove(Object key) {
        throw readOnly();
    }

    @Override
    public void putAll(Map<? extends String, ? extends JsonValue> m) {
        throw readOnly();
    }

    @Override
    public void clear() {
        throw readOnly();
    }

    @Override
    public Set<String> keySet() {
        return attributeValue.m().keySet();
    }

    @Override
    public Collection<JsonValue> values() {
        return attributeValue.m().values().stream().map(DynamoDbJsonValue::of).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Set<Entry<String, JsonValue>> entrySet() {
        return attributeValue.m().entrySet().stream().map(DynamoDbEntry::new).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ValueType getValueType() {
        return ValueType.OBJECT;
    }

    @RequiredArgsConstructor
    private static class DynamoDbEntry implements Entry<String, JsonValue> {

        private final Entry<String, AttributeValue> entry;

        @Override
        public String getKey() {
            return entry.getKey();
        }

        @Override
        public JsonValue getValue() {
            return DynamoDbJsonValue.of(entry.getValue());
        }

        @Override
        public JsonValue setValue(JsonValue value) {
            throw readOnly();
        }
    }
}
