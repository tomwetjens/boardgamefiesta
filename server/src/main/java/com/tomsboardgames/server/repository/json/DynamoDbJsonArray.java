package com.tomsboardgames.server.repository.json;

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
        return new DynamoDbJsonObject(attributeValue.l().get(index));
    }

    @Override
    public JsonArray getJsonArray(int index) {
        return new DynamoDbJsonArray(attributeValue.l().get(index));
    }

    @Override
    public JsonNumber getJsonNumber(int index) {
        return new DynamoDbJsonNumber(attributeValue.l().get(index));
    }

    @Override
    public JsonString getJsonString(int index) {
        if (attributeValue.hasSs()) {
            return new DynamoDbJsonString(attributeValue.ss().get(index));
        } else {
            return new DynamoDbJsonString(attributeValue.l().get(index));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends JsonValue> List<T> getValuesAs(Class<T> aClass) {
        if (aClass.equals(JsonObject.class)) {
            return (List<T>) attributeValue.l().stream().map(DynamoDbJsonObject::new).collect(Collectors.toList());
        } else if (aClass.equals(JsonString.class)) {
            if (attributeValue.hasSs()) {
                return (List<T>) attributeValue.ss().stream().map(DynamoDbJsonString::new).collect(Collectors.toList());
            } else {
                return (List<T>) attributeValue.l().stream().map(DynamoDbJsonString::new).collect(Collectors.toList());
            }
        } else if (aClass.equals(JsonNumber.class)) {
            if (attributeValue.hasNs()) {
                return (List<T>) attributeValue.ns().stream().map(DynamoDbJsonNumber::new).collect(Collectors.toList());
            } else {
                return (List<T>) attributeValue.l().stream().map(DynamoDbJsonNumber::new).collect(Collectors.toList());
            }
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
        if (attributeValue.hasSs()) {
            var value = attributeValue.ss().get(index);
            return value != null ? value : defaultValue;
        } else {
            var value = attributeValue.l().get(index).s();
            return value != null ? value : defaultValue;
        }
    }

    @Override
    public int getInt(int index) {
        if (attributeValue.hasNs()) {
            return Integer.parseInt(attributeValue.ns().get(index));
        } else {
            return Integer.parseInt(attributeValue.l().get(index).n());
        }
    }

    @Override
    public int getInt(int index, int defaultValue) {
        if (attributeValue.hasNs()) {
            var value = attributeValue.ns().get(index);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } else {
            var value = attributeValue.l().get(index).n();
            return value != null ? Integer.parseInt(value) : defaultValue;
        }
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
        if (attributeValue.hasSs()) {
            return attributeValue.ss().size();
        } else if (attributeValue.hasNs()) {
            return attributeValue.ns().size();
        } else {
            return attributeValue.l().size();
        }
    }

    @Override
    public boolean isEmpty() {
        if (attributeValue.hasSs()) {
            return attributeValue.ss().isEmpty();
        } else if (attributeValue.hasNs()) {
            return attributeValue.ns().isEmpty();
        } else {
            return attributeValue.l().isEmpty();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (attributeValue.hasSs()) {
            return attributeValue.ss().contains(((DynamoDbJsonString) o).getValue());
        } else if (attributeValue.hasNs()) {
            return attributeValue.ns().contains(((DynamoDbJsonNumber) o).getValue());
        } else {
            return attributeValue.l().contains(((DynamoDbJsonValue) o).getAttributeValue());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<JsonValue> iterator() {
        if (attributeValue.hasSs()) {
            return (Iterator<JsonValue>) (Iterator<? extends JsonValue>) attributeValue.ss().stream().map(DynamoDbJsonString::new).iterator();
        } else if (attributeValue.hasNs()) {
            return (Iterator<JsonValue>) (Iterator<? extends JsonValue>) attributeValue.ns().stream().map(DynamoDbJsonNumber::new).iterator();
        } else {
            return attributeValue.l().stream().map(DynamoDbJsonValue::of).iterator();
        }
    }

    @Override
    public Object[] toArray() {
        if (attributeValue.hasSs()) {
            return attributeValue.ss().stream().map(DynamoDbJsonString::new).toArray();
        } else if (attributeValue.hasNs()) {
            return attributeValue.ns().stream().map(DynamoDbJsonNumber::new).toArray();
        } else {
            return attributeValue.l().stream().map(DynamoDbJsonValue::of).toArray();
        }
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
        if (attributeValue.hasSs()) {
            return c.stream().allMatch(o -> attributeValue.ss().contains(((DynamoDbJsonString) o).getValue()));
        } else if (attributeValue.hasNs()) {
            return c.stream().allMatch(o -> attributeValue.ns().contains(((DynamoDbJsonNumber) o).getValue()));
        } else {
            return c.stream().allMatch(o -> attributeValue.l().contains(((DynamoDbJsonValue) o).getAttributeValue()));
        }
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
        if (attributeValue.hasSs()) {
            return new DynamoDbJsonValueListIterator<>(attributeValue.ss().listIterator(), DynamoDbJsonString::new);
        } else if (attributeValue.hasNs()) {
            return new DynamoDbJsonValueListIterator<>(attributeValue.ns().listIterator(), DynamoDbJsonNumber::new);
        } else {
            return new DynamoDbJsonValueListIterator<>(attributeValue.l().listIterator(), DynamoDbJsonValue::of);
        }
    }

    @Override
    public ListIterator<JsonValue> listIterator(int index) {
        if (attributeValue.hasSs()) {
            return new DynamoDbJsonValueListIterator<>(attributeValue.ss().listIterator(index), DynamoDbJsonString::new);
        } else if (attributeValue.hasNs()) {
            return new DynamoDbJsonValueListIterator<>(attributeValue.ns().listIterator(index), DynamoDbJsonNumber::new);
        } else {
            return new DynamoDbJsonValueListIterator<>(attributeValue.l().listIterator(index), DynamoDbJsonValue::of);
        }
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
