package com.wetjens.gwt.server.domain;

import java.time.Instant;

import com.wetjens.gwt.server.rest.APIError;
import com.wetjens.gwt.server.rest.APIException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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

    @Getter
    @Setter(value = AccessLevel.PACKAGE)
    private com.wetjens.gwt.Player color;

    @Getter
    @Setter(value = AccessLevel.PACKAGE)
    private Integer score;

    @Getter
    @Setter(value = AccessLevel.PACKAGE)
    private Boolean winner;

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

}
