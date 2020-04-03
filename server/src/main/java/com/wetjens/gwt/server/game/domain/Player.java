package com.wetjens.gwt.server.game.domain;

import com.wetjens.gwt.server.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Builder
public class Player {

    @Getter
    private final User.Id userId;

    @Getter
    private Status status;

    public enum Status {
        INVITED,
        ACCEPTED,
        REJECTED;
    }

}
