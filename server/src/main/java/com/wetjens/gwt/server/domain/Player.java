package com.wetjens.gwt.server.domain;

import java.time.Instant;
import java.util.UUID;

import com.wetjens.gwt.server.rest.APIError;
import com.wetjens.gwt.server.rest.APIException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

@Builder
public class Player {

    @Getter
    @NonNull
    private final Id id;

    @Getter
    @NonNull
    private final Type type;

    @Getter
    private final User.Id userId;

    @Getter
    @NonNull
    private Status status;

    @Getter
    @NonNull
    private Instant created;

    @Getter
    @NonNull
    private Instant updated;

    @Getter
    @Setter(value = AccessLevel.PACKAGE)
    private Score score;

    @Getter
    @Setter(value = AccessLevel.PACKAGE)
    private Boolean winner;

    static Player accepted(User.Id userId) {
        Instant created = Instant.now();

        return Player.builder()
                .id(Id.generate())
                .type(Type.USER)
                .userId(userId)
                .status(Status.ACCEPTED)
                .created(created)
                .updated(created)
                .build();
    }

    static Player invite(User.Id userId) {
        Instant invited = Instant.now();

        return Player.builder()
                .id(Id.generate())
                .type(Type.USER)
                .userId(userId)
                .status(Status.INVITED)
                .created(invited)
                .updated(invited)
                .build();
    }

    static Player computer() {
        Instant created = Instant.now();

        return Player.builder()
                .id(Id.generate())
                .type(Type.COMPUTER)
                .status(Status.ACCEPTED)
                .created(created)
                .updated(created)
                .build();
    }

    void accept() {
        if (status != Status.INVITED) {
            throw APIException.badRequest(APIError.ALREADY_RESPONDED);
        }

        status = Status.ACCEPTED;
        updated = Instant.now();
    }

    void reject() {
        if (status != Status.INVITED) {
            throw APIException.badRequest(APIError.ALREADY_RESPONDED);
        }

        status = Status.REJECTED;
        updated = Instant.now();
    }

    public enum Status {
        INVITED,
        ACCEPTED,
        REJECTED;
    }

    public enum Type {
        USER,
        COMPUTER
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;

        private static Player.Id generate() {
            return of(UUID.randomUUID().toString());
        }
    }
}
