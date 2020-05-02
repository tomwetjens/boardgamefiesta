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

    @NonNull
    Game.Id gameId;

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

    public LogEntry(@NonNull Game game, @NonNull GWTEvent event) {
        this.gameId = game.getId();
        this.playerId = game.getPlayerByColor(event.getPlayer()).map(Player::getId).orElse(null);
        this.timestamp = Instant.now();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = event.getType().name();
        this.values = event.getValues().stream()
                .map(value -> {
                    if (value instanceof Player) {
                        return game.getPlayerByColor((com.wetjens.gwt.Player) value)
                                .map(Player::getId)
                                .orElse(null);
                    } else if (value instanceof Action) {
                        return ActionType.of(((Action) value).getClass()).name();
                    } else if (value instanceof Enum<?>) {
                        return value.toString();
                    }
                    return value;
                })
                .collect(Collectors.toList());
    }

    public LogEntry(@NonNull Game game, @NonNull User.Id userId, Type type, List<Object> values) {
        this.gameId = game.getId();
        this.playerId = game.getPlayerByUserId(userId).map(Player::getId).orElse(null);
        this.timestamp = Instant.now();
        this.expires = this.timestamp.plus(DEFAULT_RETENTION);
        this.type = type.name();
        this.values = values;
    }

    public enum Type {
        ACCEPT,
        REJECT,
        START,
        INVITE,
        CREATE
    }
}
