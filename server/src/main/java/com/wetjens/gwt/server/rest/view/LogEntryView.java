package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.api.Action;
import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.LogEntry;
import com.wetjens.gwt.server.domain.User;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class LogEntryView {

    Instant timestamp;
    LogEntry.Type type;
    List<String> parameters;

    PlayerView player;

    @SuppressWarnings("unchecked")
    public LogEntryView(@NonNull Game game, @NonNull LogEntry logEntry, @NonNull Map<User.Id, User> userMap) {
        this.timestamp = logEntry.getTimestamp();
        this.type = logEntry.getType();
        this.parameters = logEntry.getParameters().stream()
                .map(parameter -> {
                    if (parameter.startsWith("Player:")) {
                        return parameter.split(":")[1];
                    } else if (parameter.startsWith("Action:")) {
                        try {
                            return game.getImplementation().toView((Class<? extends Action>) Class.forName(parameter.split(":")[1]));
                        } catch (ClassNotFoundException e) {
                            return parameter;
                        }
                    }
                    return parameter;
                })
                .collect(Collectors.toList());

        var player = game.getPlayerById(logEntry.getPlayerId());
        this.player = new PlayerView(player, player.getUserId() != null ? userMap.get(player.getUserId()) : null);
    }

}
