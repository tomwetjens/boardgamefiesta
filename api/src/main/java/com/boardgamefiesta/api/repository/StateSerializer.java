package com.boardgamefiesta.api.repository;

import com.boardgamefiesta.api.domain.State;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

public interface StateSerializer<T extends State> {
    JsonObject serialize(T state, JsonBuilderFactory factory);
}
