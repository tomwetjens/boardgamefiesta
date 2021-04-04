package com.boardgamefiesta.domain.event;

import com.boardgamefiesta.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Builder
public class WebSocketConnection {

    @Getter
    @NonNull
    String id;

    @Getter
    @NonNull
    User.Id userId;

    @Getter
    @NonNull
    Status status;

    @Getter
    @NonNull
    Instant created;

    @Getter
    @NonNull
    Instant updated;

    public static WebSocketConnection create(String id, User.Id userId) {
        return WebSocketConnection.builder()
                .id(id)
                .userId(userId)
                .status(Status.INACTIVE)
                .created(Instant.now())
                .updated(Instant.now())
                .build();
    }

    public Instant getExpires() {
        return calculateExpires(updated);
    }

    public static Instant calculateExpires(Instant updated) {
        return updated.plus(2, ChronoUnit.MINUTES);
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
