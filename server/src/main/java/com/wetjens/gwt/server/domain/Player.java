package com.wetjens.gwt.server.domain;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Builder
public class Player {

    @Getter
    private final User.Id userId;

    @Getter
    private Status status;

    @Getter
    private Instant created;

    @Getter
    private Instant updated;

    static Player createAccepted(User.Id userId) {
        Instant created = Instant.now();

        return Player.builder()
                .userId(userId)
                .status(Status.ACCEPTED)
                .created(created)
                .updated(created)
                .build();
    }

    static Player invite(User.Id userId) {
        Instant invited = Instant.now();

        return Player.builder()
                .userId(userId)
                .status(Status.INVITED)
                .created(invited)
                .updated(invited)
                .build();
    }

    void accept() {
        if (status != Status.INVITED) {
            throw new IllegalStateException("Not invited");
        }

        status = Status.ACCEPTED;
        updated = Instant.now();
    }

    void reject() {
        if (status != Status.INVITED) {
            throw new IllegalStateException("Not invited");
        }

        status = Status.REJECTED;
        updated = Instant.now();
    }

    public enum Status {
        INVITED,
        ACCEPTED,
        REJECTED;
    }

}
