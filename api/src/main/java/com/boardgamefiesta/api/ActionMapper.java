package com.boardgamefiesta.api;

import javax.json.JsonObject;

public interface ActionMapper<T extends State> {
    Action toAction(JsonObject jsonObject, T state);
}
