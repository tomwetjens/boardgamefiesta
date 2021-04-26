package com.boardgamefiesta.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.Json;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ContinuationToken {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final SimpleEncryptor ENCRYPTOR = new SimpleEncryptor();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String from(Map<String, String> values) {
        var jsonObject = Json.createObjectBuilder((Map) values).build();
        var bytes = JsonSupport.getBytes(jsonObject);
        return ENCODER.encodeToString(ENCRYPTOR.encrypt(bytes));
    }

    public static Map<String, AttributeValue> parse(String str) {
        var bytes = Base64.getDecoder().decode(str);
        var jsonObject = JsonSupport.read(ENCRYPTOR.decrypt(bytes));
        return jsonObject.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), key -> AttributeValue.builder().s(jsonObject.getString(key)).build()));
    }

}
