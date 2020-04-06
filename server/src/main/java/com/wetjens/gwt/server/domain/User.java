package com.wetjens.gwt.server.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private static final Duration RETENTION_AFTER_LAST_SEEN = Duration.of(365, ChronoUnit.DAYS);

    @Getter
    private final Id id;

    @Getter
    private String username;

    @Getter
    private String email;

    @Getter
    private Instant created;

    @Getter
    private Instant updated;

    @Getter
    private Instant lastSeen;

    @Getter
    private Instant expires;

    public static User createAutomatically(Id id, String username, String email) {
        Instant created = Instant.now();

        return User.builder()
                .id(id)
                .created(created)
                .updated(created)
                .lastSeen(created)
                .expires(calculateExpires(created))
                .username(username)
                .email(email)
                .build();
    }

    public void changeUsername(String username) {
        this.username = username;
        updated = Instant.now();
    }

    public void confirmEmail(String email) {
        this.email = email;
        updated = Instant.now();
    }

    public static Instant calculateExpires(Instant lastSeen) {
        return lastSeen.plus(RETENTION_AFTER_LAST_SEEN);
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;
    }
}
