package com.wetjens.gwt.server.user.domain;

import lombok.Getter;
import lombok.Value;

public class User {

    @Getter
    private final Id id;

    public User(Id id) {
        this.id = id;
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;
    }
}
