package com.wetjens.gwt.server.domain;

import java.time.Instant;
import java.util.List;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.GWTEvent;
import lombok.NonNull;
import lombok.Value;

@Value
public class GameLogEntry {

    @NonNull
    Game.Id gameId;

    @NonNull
    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    GameLogEntryType type;

    List<Object> values;

    public static GameLogEntry of(User user, GWTEvent event, List<Object> values) {
        // TODO
        return null;
    }
}
