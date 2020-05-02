package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.LogEntry;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
public class LogEntryView {

    Instant timestamp;
    PlayerView player;
    String type;
    List<Object> values;

    public LogEntryView(LogEntry logEntry, Map<Player.Id, Player> playerMap, Map<User.Id, User> userMap) {
        this.timestamp = logEntry.getTimestamp();
        var player = playerMap.get(logEntry.getPlayerId());
        this.player = new PlayerView(player, player.getUserId() != null ? userMap.get(player.getUserId()) : null);
        this.type = logEntry.getType();
        this.values = logEntry.getValues();
    }

}
