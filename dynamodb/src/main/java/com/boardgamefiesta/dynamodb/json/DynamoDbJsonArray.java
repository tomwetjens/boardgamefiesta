package com.boardgamefiesta.dynamodb.json;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DynamoDbJsonArray extends DynamoDbJsonValue implements JsonArray {

    @Getter(AccessLevel.PACKAGE)
    private final AttributeValue attributeValue;

    @Override
    public JsonObject getJsonObject(int index) {
        return DynamoDbJsonValue.of(attributeValue.l().get(index)).asJsonObject();
    }

    @Override
    public JsonArray getJsonArray(int index) {
        return DynamoDbJsonValue.of(attributeValue.l().get(index)).asJsonArray();
    }

    @Override
    public JsonNumber getJsonNumber(int index) {
        JsonValue jsonValue = DynamoDbJsonValue.of(attributeValue.l().get(index));
        if (jsonValue == JsonValue.NULL) {
            throw new NullPointerException();
        }
        return (JsonNumber) jsonValue;
    }

    @Override
    public JsonString getJsonString(int index) {
        JsonValue jsonValue = DynamoDbJsonValue.of(attributeValue.l().get(index));
        if (jsonValue == JsonValue.NULL) {
            throw new NullPointerException();
        }
        return (JsonString) jsonValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends JsonValue> List<T> getValuesAs(Class<T> aClass) {
        if (aClass.equals(JsonObject.class)) {
            return (List<T>) attributeValue.l().stream().map(DynamoDbJsonObject::new).collect(Collectors.toList());
        } else if (aClass.equals(JsonString.class)) {
            return (List<T>) attributeValue.l().stream().map(DynamoDbJsonString::new).collect(Collectors.toList());
        } else if (aClass.equals(JsonNumber.class)) {
            return (List<T>) attributeValue.l().stream().map(DynamoDbJsonNumber::new).collect(Collectors.toList());
        } else {
            throw new UnsupportedOperationException("Unsupported class: " + aClass);
        }
    }

    private static UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("JsonObjects from DynamoDB are read only views on AttributeValue");
    }

    @Override
    public String getString(int index) {
        return getString(index, null);
    }

    @Override
    public String getString(int index, String defaultValue) {
        var value = attributeValue.l().get(index).s();
        return value != null ? value : defaultValue;
    }

    @Override
    public int getInt(int index) {
        return Integer.parseInt(attributeValue.l().get(index).n());
    }

    @Override
    public int getInt(int index, int defaultValue) {
        var value = attributeValue.l().get(index).n();
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    @Override
    public boolean getBoolean(int index) {
        return attributeValue.l().get(index).bool();
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        var value = attributeValue.l().get(index).bool();
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean isNull(int index) {
        var attributeValue = this.attributeValue.l().get(index);
        return attributeValue == null || attributeValue.nul();
    }

    @Override
    public int size() {
        return attributeValue.l().size();
    }

    @Override
    public boolean isEmpty() {
        return attributeValue.l().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return attributeValue.l().contains(((DynamoDbJsonValue) o).getAttributeValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<JsonValue> iterator() {
        return attributeValue.l().stream().map(DynamoDbJsonValue::of).iterator();
    }

    @Override
    public Object[] toArray() {
        return attributeValue.l().stream().map(DynamoDbJsonValue::of).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(JsonValue jsonValue) {
        throw readOnly();
    }

    @Override
    public boolean remove(Object o) {
        throw readOnly();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(o -> attributeValue.l().contains(((DynamoDbJsonValue) o).getAttributeValue()));
    }

    @Override
    public boolean addAll(Collection<? extends JsonValue> c) {
        throw readOnly();
    }

    @Override
    public boolean addAll(int index, Collection<? extends JsonValue> c) {
        throw readOnly();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw readOnly();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw readOnly();
    }

    @Override
    public void clear() {
        throw readOnly();
    }

    @Override
    public JsonValue get(int index) {
        return DynamoDbJsonValue.of(attributeValue.l().get(index));
    }

    @Override
    public JsonValue set(int index, JsonValue element) {
        throw readOnly();
    }

    @Override
    public void add(int index, JsonValue element) {
        throw readOnly();
    }

    @Override
    public JsonValue remove(int index) {
        throw readOnly();
    }

    @Override
    public int indexOf(Object o) {
        return attributeValue.l().indexOf(((DynamoDbJsonValue) o).getAttributeValue());
    }

    @Override
    public int lastIndexOf(Object o) {
        return attributeValue.l().lastIndexOf(((DynamoDbJsonValue) o).getAttributeValue());
    }

    @Override
    public ListIterator<JsonValue> listIterator() {
        return new DynamoDbJsonValueListIterator<>(attributeValue.l().listIterator(), DynamoDbJsonValue::of);
    }

    @Override
    public ListIterator<JsonValue> listIterator(int index) {
        return new DynamoDbJsonValueListIterator<>(attributeValue.l().listIterator(index), DynamoDbJsonValue::of);
    }

    @Override
    public List<JsonValue> subList(int fromIndex, int toIndex) {
        return attributeValue.l().subList(fromIndex, toIndex).stream().map(DynamoDbJsonValue::of).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ARRAY;
    }

    @RequiredArgsConstructor
    private static class DynamoDbJsonValueListIterator<T> implements ListIterator<JsonValue> {

        private final ListIterator<T> iterator;
        private final Function<T, JsonValue> mapper;

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public JsonValue next() {
            var attributeValue = iterator.next();
            return mapper.apply(attributeValue);
        }

        @Override
        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        @Override
        public JsonValue previous() {
            var attributeValue = iterator.previous();
            return mapper.apply(attributeValue);
        }

        @Override
        public int nextIndex() {
            return iterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return iterator.previousIndex();
        }

        @Override
        public void remove() {
            throw readOnly();
        }

        @Override
        public void set(JsonValue jsonValue) {
            throw readOnly();
        }

        @Override
        public void add(JsonValue jsonValue) {
            throw readOnly();
        }
    }
}
