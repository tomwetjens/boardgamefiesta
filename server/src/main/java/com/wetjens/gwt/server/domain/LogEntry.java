package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.api.InGameEvent;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LogEntry {

    private static final Duration DEFAULT_RETENTION = Duration.of(365, ChronoUnit.DAYS);

    private static final ThreadLocal<Instant> THREAD_LAST_TIMESTAMP = new ThreadLocal<>();

    @NonNull
    Player.Id playerId;

    @NonNull
    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    Instant expires;

    @NonNull
    Type type;

    @NonNull
    List<String> parameters;

    LogEntry(@NonNull Table table, @NonNull InGameEvent event) {
        this.playerId = Player.Id.of(event.getPlayer().getName());
        this.userId = table.getPlayerById(this.playerId).map(Player::getUserId).orElse(null);
        this.timestamp = generateTimestamp();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = Type.IN_GAME_EVENT;
        this.parameters = Stream.concat(Stream.of(event.getType()), event.getParameters().stream())
                .collect(Collectors.toList());
    }


    LogEntry(@NonNull Player player, @NonNull Type type) {
        this(player, type, Collections.emptyList());
    }

    LogEntry(@NonNull Player player, @NonNull Type type, @NonNull List<Object> parameters) {
        this.playerId = player.getId();
        this.userId = player.getUserId();
        this.timestamp = generateTimestamp();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = type;
        this.parameters = parameters.stream().map(Object::toString).collect(Collectors.toList());
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
        PROPOSED_TO_LEAVE,
        AGREED_TO_LEAVE,
        CREATE,
        KICK,
        LEFT, IN_GAME_EVENT
    }
}
