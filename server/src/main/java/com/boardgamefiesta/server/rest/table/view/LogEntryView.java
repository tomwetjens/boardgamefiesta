package com.boardgamefiesta.server.rest.table.view;

import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.domain.table.LogEntry;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Value
public class LogEntryView {

    Instant timestamp;
    LogEntry.Type type;
    List<String> parameters;

    PlayerSummaryView player;
    UserSummaryView user;
    UserSummaryView otherUser;

    public LogEntryView(@NonNull Supplier<Table> tableSupplier,
                        @NonNull LogEntry logEntry,
                        @NonNull Function<User.Id, User> userFunction) {
        this.timestamp = logEntry.getTimestamp();
        this.type = logEntry.getType();
        this.parameters = logEntry.getParameters();

        this.player = logEntry.getPlayerColor()
                .map(playerColor -> new PlayerSummaryView(logEntry.getPlayerId().getId(), playerColor))
                // Only retrieve table if log entry does not contain enough information (mostly older log entries)
                .orElseGet(() -> tableSupplier.get().getPlayerById(logEntry.getPlayerId()).map(PlayerSummaryView::new).orElse(null));
        this.user = logEntry.getUserId().map(userId -> new UserSummaryView(userFunction.apply(userId))).orElse(null);

        switch (logEntry.getType()) {
            case INVITE:
            case KICK:
                var otherUserId = User.Id.of(logEntry.getParameters().get(0));
                otherUser = new UserSummaryView(userFunction.apply(otherUserId));
                break;
            default:
                otherUser = null;
        }
    }

    @Value
    public static class UserSummaryView {
        String id;
        String username;

        UserSummaryView(User user) {
            id = user.getId().getId();
            username = user.getUsername();
        }
    }

    @Value
    @AllArgsConstructor
    public static class PlayerSummaryView {
        String id;
        PlayerColor color;

        PlayerSummaryView(Player player) {
            id = player.getId().getId();
            color = player.getColor();
        }
    }

}
