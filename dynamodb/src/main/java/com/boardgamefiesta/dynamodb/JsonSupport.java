package com.boardgamefiesta.dynamodb;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

final class JsonSupport {

    public static byte[] getBytes(JsonObject jsonObject) {
        byte[] serialized;
        try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (var jsonWriter = Json.createWriter(byteArrayOutputStream)) {
                jsonWriter.writeObject(jsonObject);
            }
            serialized = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return serialized;
    }

    public static JsonObject read(byte[] bytes) {
        try (var jsonReader = Json.createReader(new ByteArrayInputStream(bytes))) {
            return jsonReader.readObject();
        }
    }
}
