package com.wetjens.gwt.server.domain;

import lombok.Getter;
import lombok.Value;

public class User {

    @Getter
    private final Id id;

    public User(Id id) {
        this.id = id;
    }

    public static User createAutomatically(Id id) {
        return new User(id);
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;
    }
}
