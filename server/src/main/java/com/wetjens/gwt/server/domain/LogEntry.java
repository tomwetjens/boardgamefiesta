package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.GWTEvent;
import com.wetjens.gwt.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LogEntry {

    private static final Duration DEFAULT_RETENTION = Duration.of(1, ChronoUnit.YEARS);

    @NonNull
    Game.Id gameId;

    @NonNull
    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    Instant expires;

    @NonNull
    Type type;

    @NonNull
    List<Object> values;

    public LogEntry(@NonNull Game.Id gameId, GWTEvent event, Map<Player, User> playerUserMap) {
        this.gameId = gameId;
        this.userId = playerUserMap.get(event.getPlayer()).getId();
        this.timestamp = Instant.now();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = Type.EVENT;
        this.values = event.getValues().stream()
                .map(value -> {
                    if (value instanceof Player) {
                        return playerUserMap.get(value).getUsername();
                    } else if (value instanceof Action) {
                        return ActionType.of(((Action) value).getClass()).name();
                    } else if (value instanceof Enum<?>) {
                        return value.toString();
                    }
                    return value;
                })
                .collect(Collectors.toList());
    }

    public LogEntry(@NonNull Game.Id gameId, @NonNull User.Id userId, Type type, List<Object> values) {
        this.gameId = gameId;
        this.userId = userId;
        this.timestamp = Instant.now();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = type;
        this.values = values;
    }

    public enum Type {
        SKIP,
        ACCEPT,
        REJECT,
        START,
        END_TURN,
        INVITE,
        EVENT
    }
}
