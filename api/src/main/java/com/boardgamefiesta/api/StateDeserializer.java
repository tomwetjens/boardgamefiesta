package com.boardgamefiesta.api;

import javax.json.JsonObject;

public interface StateDeserializer<T extends State> {
    T deserialize(JsonObject jsonObject);
}
