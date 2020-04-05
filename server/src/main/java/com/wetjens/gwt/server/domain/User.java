package com.wetjens.gwt.server.domain;

import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    @Getter
    private final Id id;

    @Getter
    private String username;

    @Getter
    private String email;

    public static <T> User createAutomatically(Id id, String username, String email) {
        return new User(id, username, email);
    }

    public void changeUsername(String username) {
        this.username = username;
    }

    public void confirmEmail(String email) {
        this.email = email;
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;
    }
}
