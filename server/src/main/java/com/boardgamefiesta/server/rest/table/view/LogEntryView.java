package com.boardgamefiesta.server.rest.table.view;

import com.boardgamefiesta.domain.table.LogEntry;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Value
public class LogEntryView {

    Instant timestamp;
    LogEntry.Type type;
    List<String> parameters;

    UserView user;
    PlayerView player;
    UserView otherUser;

    public LogEntryView(@NonNull Table table,
                        @NonNull LogEntry logEntry,
                        @NonNull Function<User.Id, User> userFunction,
                        @NonNull Map<User.Id, Rating> ratingMap) {
        this.timestamp = logEntry.getTimestamp();
        this.type = logEntry.getType();
        this.parameters = logEntry.getParameters();
        this.player = table.getPlayerById(logEntry.getPlayerId())
                .map(player -> new PlayerView(player, userFunction, ratingMap))
                .orElse(null);
        this.user = logEntry.getUserId().map(userId -> new UserView(userId, userFunction.apply(userId), null)).orElse(null);

        switch (logEntry.getType()) {
            case INVITE:
            case KICK:
                var otherUserId = User.Id.of(logEntry.getParameters().get(0));
                otherUser = new UserView(otherUserId, userFunction.apply(otherUserId), null);
                break;
            default:
                otherUser = null;
        }
    }

}
