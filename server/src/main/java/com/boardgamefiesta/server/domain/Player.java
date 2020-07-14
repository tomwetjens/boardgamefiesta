package com.boardgamefiesta.server.domain;

import com.boardgamefiesta.api.domain.PlayerColor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Builder
public class Player {

    @Getter
    @NonNull
    private final Id id;

    @Getter
    @NonNull
    private final Type type;

    private final User.Id userId;

    @Getter
    @NonNull
    private final Instant created;

    @Getter
    @NonNull
    private Status status;

    @Getter
    @NonNull
    private Instant updated;

    @Getter
    private PlayerColor color;

    private Instant turnLimit;

    private Integer score;

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

    void assignScore(int score, boolean winner) {
        this.score = score;
        this.winner = winner;

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

    public void beginTurn(Duration timeLimit) {
        this.turnLimit = Instant.now().plus(timeLimit);
        this.updated = Instant.now();
    }

    public void endTurn() {
        this.turnLimit = null;
        this.updated = Instant.now();
    }

    public Optional<User.Id> getUserId() {
        return Optional.ofNullable(userId);
    }

    public Optional<Integer> getScore() {
        return Optional.ofNullable(score);
    }

    public Optional<Boolean> getWinner() {
        return Optional.ofNullable(winner);
    }

    public Optional<Instant> getTurnLimit() {
        return Optional.ofNullable(turnLimit);
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
