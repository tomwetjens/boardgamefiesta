package com.tomsboardgames.server.domain;

import lombok.*;
import org.apache.commons.codec.binary.Hex;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private static final Duration RETENTION_AFTER_LAST_SEEN = Duration.of(365, ChronoUnit.DAYS);

    public static final String DEFAULT_LANGUAGE = "en";

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

    @Getter
    private String language;

    public static User createAutomatically(@NonNull Id id, @NonNull String username, @NonNull String email) {
        var created = Instant.now();

        return User.builder()
                .id(id)
                .created(created)
                .updated(created)
                .lastSeen(created)
                .expires(calculateExpires(created))
                .username(username)
                .email(email)
                .language(DEFAULT_LANGUAGE)
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

    public void changeLanguage(String language) {
        this.language = language;
        updated = Instant.now();
    }

    public URI getAvatarUrl() {
        return getGravatarUrl(email);
    }

    public static Instant calculateExpires(Instant lastSeen) {
        return lastSeen.plus(RETENTION_AFTER_LAST_SEEN);
    }

    @Value(staticConstructor = "of")
    public static class Id implements Serializable {
        private static final long serialVersionUID = 1L;

        String id;
    }

    private static URI getGravatarUrl(String email) {
        String hash = md5String(email.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII));
        return URI.create("https://www.gravatar.com/avatar/" + hash + "?s=48&d=identicon&r=g");
    }

    private static String md5String(byte[] bytes) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(bytes);
            return Hex.encodeHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
