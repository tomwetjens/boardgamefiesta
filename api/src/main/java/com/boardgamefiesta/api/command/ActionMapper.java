package com.boardgamefiesta.api.command;

import com.boardgamefiesta.api.domain.Action;
import com.boardgamefiesta.api.domain.State;

import javax.json.JsonObject;

public interface ActionMapper<T extends State> {
    /**
     * @throws javax.json.JsonException when JSON could not be parsed into an action.
     */
    Action toAction(JsonObject jsonObject, T state);
}
