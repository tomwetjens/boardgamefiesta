package com.boardgamefiesta.server.event.domain;

import com.boardgamefiesta.server.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import javax.websocket.Session;
import java.time.Instant;

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

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
