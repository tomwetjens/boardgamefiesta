package com.boardgamefiesta.api.repository;

import com.boardgamefiesta.api.domain.State;

import javax.json.JsonObject;

public interface StateDeserializer<T extends State> {
    T deserialize(JsonObject jsonObject);
}
