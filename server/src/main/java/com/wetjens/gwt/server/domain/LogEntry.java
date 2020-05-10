package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.GWTEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LogEntry {

    private static final Duration DEFAULT_RETENTION = Duration.of(365, ChronoUnit.DAYS);

    private static final ThreadLocal<Instant> THREAD_LAST_TIMESTAMP = new ThreadLocal<>();

    @NonNull
    Player.Id playerId;

    @NonNull
    Instant timestamp;

    @NonNull
    Instant expires;

    @NonNull
    String type;

    @NonNull
    List<Object> values;

    LogEntry(@NonNull Game game, @NonNull GWTEvent event) {
        this.playerId = game.getPlayerById(Player.Id.of(event.getPlayer().getName())).getId();
        this.timestamp = generateTimestamp();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = event.getType().name();
        this.values = event.getValues().stream()
                .map(value -> {
                    if (value instanceof com.wetjens.gwt.Player) {
                        return game.getPlayerById(Player.Id.of(((com.wetjens.gwt.Player) value).getName())).getId();
                    } else if (value instanceof Action) {
                        return ActionType.of(((Action) value).getClass()).name();
                    } else if (value instanceof Enum<?>) {
                        return value.toString();
                    }
                    return value;
                })
                .collect(Collectors.toList());
    }

    LogEntry(@NonNull Game game, @NonNull User.Id userId, Type type, List<Object> values) {
        this.playerId = game.getPlayerByUserId(userId).map(Player::getId).orElse(null);
        this.timestamp = generateTimestamp();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = type.name();
        this.values = values;
    }

    private static Instant generateTimestamp() {
        // Timestamps are used as key, and must be unique, even when multiple log entries are created at once
        final var now = Instant.now();

        var lastTimestamp = THREAD_LAST_TIMESTAMP.get();

        Instant timestamp;
        if (lastTimestamp == null || lastTimestamp.toEpochMilli() < now.toEpochMilli()) {
            timestamp = now;
        } else {
            timestamp = lastTimestamp.plusMillis(1);
        }

        THREAD_LAST_TIMESTAMP.set(timestamp);

        return timestamp;
    }

    public enum Type {
        ACCEPT,
        REJECT,
        START,
        INVITE,
        CREATE
    }
}
