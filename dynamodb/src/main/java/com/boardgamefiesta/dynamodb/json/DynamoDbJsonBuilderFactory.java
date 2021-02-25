package com.boardgamefiesta.dynamodb.json;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import java.util.Map;

class DynamoDbJsonBuilderFactory implements JsonBuilderFactory {

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return new DynamoDbJsonObjectBuilder();
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return new DynamoDbJsonArrayBuilder();
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        throw new UnsupportedOperationException();
    }
}
