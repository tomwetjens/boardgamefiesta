/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.user;

import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.DomainEvent;
import lombok.*;
import org.apache.commons.codec.binary.Hex;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User implements AggregateRoot {

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
    @Builder.Default
    private final int version = 1;

    @Getter
    @NonNull
    @Builder.Default
    private final Instant created = Instant.now();

    @Getter
    @NonNull
    private String cognitoUsername;

    @Getter
    @NonNull
    private String username;

    @Getter
    @NonNull
    private String email;

    private String location;

    @Getter
    @NonNull
    @Builder.Default
    private Instant updated = Instant.now();

    @Getter
    @NonNull
    @Builder.Default
    private String language = DEFAULT_LANGUAGE;

    private ZoneId timeZone;

    @Getter
    private boolean deleted;

    @Getter
    @NonNull
    @Builder.Default
    private final EmailPreferences emailPreferences = new EmailPreferences();

    @Getter
    @NonNull
    @Builder.Default
    private final ColorPreferences colorPreferences = new ColorPreferences();

    public static User createAutomatically(@NonNull String cognitoUsername, @NonNull String email) {
        var created = Instant.now();

        return User.builder()
                .id(Id.generate())
                .version(1)
                .created(created)
                .updated(created)
                .cognitoUsername(cognitoUsername)
                .username(cognitoUsername)
                .email(email)
                .language(DEFAULT_LANGUAGE)
                .build();
    }

    public static void validateUsername(@NonNull String username) {
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new UsernameTooShort("Username must be at least " + MIN_USERNAME_LENGTH + " characters");
        }

        if (username.length() > MAX_USER_NAME_LENGTH) {
            throw new UsernameTooLong("Username must not exceed " + MAX_USER_NAME_LENGTH + " characters");
        }

        if (!USERNAME_VALIDATOR.matcher(username).matches()) {
            throw new UsernameInvalidChars("Username contains invalid characters");
        }

        if (FORBIDDEN_USERNAMES.contains(username.toLowerCase())
                || BAD_WORDS.stream().anyMatch(word -> username.toLowerCase().contains(word))
                || BAD_USERNAME_WORDS.stream().anyMatch(word -> username.toLowerCase().contains(word))) {
            throw new UsernameForbidden("Username is reserved or contains forbidden words");
        }
    }

    public void changeEmail(String email) {
        this.email = email;
        this.updated = Instant.now();

        new EmailChanged(id, cognitoUsername, email).fire();
    }

    public void changePassword(String password) {
        new PasswordChanged(cognitoUsername, password).fire();
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

    public Locale getLocale() {
        if (language != null) {
            if (location != null) {
                return Locale.forLanguageTag(language + "-" + location);
            } else {
                return Locale.forLanguageTag(language);
            }
        } else {
            if (location != null) {
                return Locale.forLanguageTag("en-" + location);
            } else {
                return Locale.forLanguageTag("en-US");
            }
        }
    }

    public ZoneId getTimeZone() {
        try {
            return timeZone != null ? timeZone : ZoneId.systemDefault();
        } catch (DateTimeException e) {
            return ZoneId.systemDefault();
        }
    }

    public void changeTimeZone(@NonNull ZoneId timeZone) {
        this.timeZone = timeZone;
        this.updated = Instant.now();
    }

    public void changeUsername(@NonNull String username) {
        validateUsername(username);

        this.username = username;
        this.updated = Instant.now();

        new UsernameChanged(cognitoUsername, username).fire();
    }

    public void markDeleted() {
        deleted = true;
        updated = Instant.now();

        new Deleted(cognitoUsername).fire();

        username = "*deleted*";
        email = "*deleted*";
        cognitoUsername = "*deleted*";
        location = null;
        timeZone = null;
        language = DEFAULT_LANGUAGE;
    }

    @Value(staticConstructor = "fromString")
    public static class Id {
        private static final Pattern UUID_PATTERN = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");

        String id;

        public static boolean check(String str) {
            return UUID_PATTERN.matcher(str).matches();
        }

        public static Id generate() {
            return fromString(UUID.randomUUID().toString());
        }

        /**
         * @deprecated For backwards compatibility. Use {@link #fromString(String)} instead.
         */
        @Deprecated
        public static User.Id of(String str) {
            return fromString(str);
        }
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
        User.Id userId;
        String cognitoUsername;
        String email;
    }

    @Value
    public static class PasswordChanged implements DomainEvent {
        String cognitoUsername;
        String password;
    }

    @Value
    public static class UsernameChanged implements DomainEvent {
        String cognitoUsername;
        String username;
    }

    @Value
    public static class Deleted implements DomainEvent {
        String cognitoUsername;
    }

    public static final class UsernameTooShort extends InvalidCommandException {
        public UsernameTooShort(String message) {
            super("USERNAME_TOO_SHORT", message);
        }
    }

    public static final class UsernameTooLong extends InvalidCommandException {
        public UsernameTooLong(String message) {
            super("USERNAME_TOO_LONG", message);
        }
    }

    public static final class UsernameInvalidChars extends InvalidCommandException {
        public UsernameInvalidChars(String message) {
            super("USERNAME_INVALID_CHARS", message);
        }
    }

    public static final class UsernameForbidden extends InvalidCommandException {
        public UsernameForbidden(String message) {
            super("USERNAME_FORBIDDEN", message);
        }
    }

    public static class UsernameAlreadyInUse extends InvalidCommandException {
        public UsernameAlreadyInUse() {
            super("USERNAME_ALREADY_IN_USE");
        }
    }
}
