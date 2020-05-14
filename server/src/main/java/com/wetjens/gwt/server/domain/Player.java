package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.api.PlayerColor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
public class Player {

    private static final Duration ACTION_TIMEOUT = Duration.of(3, ChronoUnit.MINUTES);

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
    private PlayerColor color;

    @Getter
    private Instant mustRespondBefore;

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

    void assignColor(PlayerColor color) {
        this.color = color;
        updated = Instant.now();
    }

    void leave() {
        if (status != Status.ACCEPTED && status != Status.PROPOSED_TO_LEAVE && status != Status.AGREED_TO_LEAVE) {
            throw APIException.badRequest(APIError.NOT_ACCEPTED);
        }

        status = Status.LEFT;
        updated = Instant.now();
    }

    void proposeToLeave() {
        if (status != Status.ACCEPTED) {
            throw APIException.badRequest(APIError.NOT_ACCEPTED);
        }

        status = Status.PROPOSED_TO_LEAVE;
        updated = Instant.now();
    }

    public void agreeToLeave() {
        if (status != Status.ACCEPTED) {
            throw APIException.badRequest(APIError.NOT_ACCEPTED);
        }

        status = Status.AGREED_TO_LEAVE;
        updated = Instant.now();
    }

    public boolean hasAccepted() {
        return status == Status.ACCEPTED || status == Status.PROPOSED_TO_LEAVE || status == Status.AGREED_TO_LEAVE;
    }

    public boolean hasResponded() {
        return status != Status.INVITED;
    }

    public boolean hasAgreedToLeave() {
        return status == Status.PROPOSED_TO_LEAVE || status == Status.AGREED_TO_LEAVE;
    }

    public void beginTurn() {
        mustRespondBefore = Instant.now().plus(ACTION_TIMEOUT);
    }

    public void endTurn() {

    }

    public enum Status {
        INVITED,
        ACCEPTED,
        REJECTED,
        LEFT,
        PROPOSED_TO_LEAVE,
        AGREED_TO_LEAVE
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
