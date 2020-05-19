package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.LogEntry;
import com.wetjens.gwt.server.domain.Table;
import com.wetjens.gwt.server.domain.User;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
public class LogEntryView {

    Instant timestamp;
    LogEntry.Type type;
    List<String> parameters;

    UserView user;
    PlayerView player;

    public LogEntryView(@NonNull Table table, @NonNull LogEntry logEntry, @NonNull Map<User.Id, User> userMap) {
        this.timestamp = logEntry.getTimestamp();
        this.type = logEntry.getType();
        this.parameters = logEntry.getParameters();
        this.player = table.getPlayerById(logEntry.getPlayerId())
                .map(player -> new PlayerView(player, player.getUserId() != null ? userMap.get(player.getUserId()) : null))
                .orElse(null);
        this.user = logEntry.getUserId().map(userId -> new UserView(userId, userMap.get(userId), null)).orElse(null);
    }

}
