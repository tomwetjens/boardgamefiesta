package com.boardgamefiesta.api;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

public interface StateSerializer<T extends State> {
    JsonObject serialize(T state, JsonBuilderFactory factory);
}
