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
import java.util.Set;
import java.util.regex.Pattern;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private static final Duration RETENTION_AFTER_LAST_SEEN = Duration.of(365, ChronoUnit.DAYS);

    public static final String DEFAULT_LANGUAGE = "en";

    private static final Set<String> FORBIDDEN_WORDS = Set.of(
            // TODO Add forbidden words
    );
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USER_NAME_LENGTH = 20;
    private static final Pattern USERNAME_VALID_CHARS = Pattern.compile("[A-Za-z0-9_\\-]+");

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

    public static void validateUsername(@NonNull String username) {
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw APIException.badRequest(APIError.USERNAME_TOO_SHORT);
        }
        if (username.length() > MAX_USER_NAME_LENGTH) {
            throw APIException.badRequest(APIError.USERNAME_TOO_LONG);
        }
        if (!USERNAME_VALID_CHARS.matcher(username).matches()) {
            throw APIException.badRequest(APIError.USERNAME_INVALID_CHARS);
        }
        if (FORBIDDEN_WORDS.stream().anyMatch(forbiddenWord -> username.toLowerCase().contains(forbiddenWord.toLowerCase()))) {
            throw APIException.badRequest(APIError.USERNAME_FORBIDDEN);
        }
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
