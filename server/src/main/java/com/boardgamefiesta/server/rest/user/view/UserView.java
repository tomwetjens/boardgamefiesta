package com.boardgamefiesta.server.rest.user.view;

import com.boardgamefiesta.domain.user.EmailPreferences;
import com.boardgamefiesta.domain.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class UserView {

    String id;
    String username;
    String avatarUrl;
    String language;
    String email;
    String location;
    String timeZone;
    EmailPreferencesView emailPreferences;

    public UserView(User user) {
        this(user.getId(), user, null);
    }

    public UserView(User.Id userId, User user, User.Id viewer) {
        this.id = userId.getId();

        if (user != null) {
            this.username = user.getUsername();
            this.avatarUrl = user.getAvatarUrl().toString();
            this.language = user.getLanguage();
            this.location = user.getLocation().orElse(null);
            this.timeZone = user.getTimeZone().getId();

            if (user.getId().equals(viewer)) {
                this.email = user.getEmail();

                this.emailPreferences = new EmailPreferencesView(user.getEmailPreferences());
            }
        }
    }
}
