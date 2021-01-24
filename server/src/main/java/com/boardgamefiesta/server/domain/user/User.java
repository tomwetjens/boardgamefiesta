package com.boardgamefiesta.server.domain.user;

import com.boardgamefiesta.ResourceLoader;
import com.boardgamefiesta.server.domain.APIError;
import com.boardgamefiesta.server.domain.APIException;
import com.boardgamefiesta.server.domain.DomainEvent;
import lombok.*;
import org.apache.commons.codec.binary.Hex;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private static final Duration RETENTION_AFTER_LAST_SEEN = Duration.of(1095, ChronoUnit.DAYS);

    public static final String DEFAULT_LANGUAGE = "en";

    private static final List<String> BAD_WORDS = ResourceLoader.readLines(User.class.getResourceAsStream("/bad_words.txt"));
    private static final List<String> FORBIDDEN_USERNAMES = ResourceLoader.readLines(User.class.getResourceAsStream("/reserved_usernames.txt"));
    private static final List<String> BAD_USERNAME_WORDS = ResourceLoader.readLines(User.class.getResourceAsStream("/bad_username_words.txt"));

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USER_NAME_LENGTH = 20;
    private static final Pattern USERNAME_VALIDATOR = Pattern.compile("[A-Za-z0-9_\\-]+");

    @Getter
    @NonNull
    private final Id id;

    @Getter
    private final Integer version;

    @Getter
    @NonNull
    private final Instant created;

    @Getter
    @NonNull
    private String username;

    @Getter
    @NonNull
    private String email;

    private String location;

    @Getter
    @NonNull
    private Instant updated;

    @Getter
    @NonNull
    private Instant lastSeen;

    @Getter
    @NonNull
    private Instant expires;

    @Getter
    @NonNull
    private String language;

    private ZoneId timeZone;

    public static User createAutomatically(@NonNull Id id, @NonNull String username, @NonNull String email) {
        validateBeforeCreate(username);

        var created = Instant.now();

        return User.builder()
                .id(id)
                .version(1)
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

        if (!USERNAME_VALIDATOR.matcher(username).matches()) {
            throw APIException.badRequest(APIError.USERNAME_INVALID_CHARS);
        }

        if (FORBIDDEN_USERNAMES.contains(username.toLowerCase())
                || BAD_WORDS.stream().anyMatch(word -> username.toLowerCase().contains(word))
                || BAD_USERNAME_WORDS.stream().anyMatch(word -> username.toLowerCase().contains(word))) {
            throw APIException.badRequest(APIError.USERNAME_FORBIDDEN);
        }
    }

    public static void validateBeforeCreate(@NonNull String username) {
        User.validateUsername(username);
    }

    public void changeEmail(String email) {
        this.email = email;
        this.updated = Instant.now();

        new EmailChanged(username, email).fire();
    }

    public void changePassword(String password) {
        new PasswordChanged(username, password).fire();
    }

    public void changeLanguage(String language) {
        this.language = language;
        this.updated = Instant.now();
    }

    public void changeLocation(String location) {
        this.location = location;
        this.updated = Instant.now();
    }

    public Optional<String> getLocation() {
        return Optional.ofNullable(location);
    }

    public URI getAvatarUrl() {
        return getGravatarUrl(email);
    }

    public void lastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
        this.expires = calculateExpires(lastSeen);
        this.updated = Instant.now();
    }

    public Locale getLocale() {
        return language != null ? Locale.forLanguageTag(language) : Locale.ENGLISH;
    }

    public ZoneId getTimeZone() {
        try {
            return timeZone != null ? timeZone : ZoneId.systemDefault();
        } catch (DateTimeException e) {
            return ZoneId.systemDefault();
        }
    }

    private static Instant calculateExpires(Instant lastSeen) {
        return lastSeen.plus(RETENTION_AFTER_LAST_SEEN);
    }

    public void changeTimeZone(@NonNull ZoneId timeZone) {
        this.timeZone = timeZone;
        this.updated = Instant.now();
    }

    @Value(staticConstructor = "of")
    public static class Id {
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

    @Value
    public static class EmailChanged implements DomainEvent {
        String username;
        String email;
    }

    @Value
    public static class PasswordChanged implements DomainEvent {
        String username;
        String password;
    }
}
