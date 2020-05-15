package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class UserView {

    String id;
    String username;
    Instant lastSeen;
    String avatarUrl;
    String language;
    String email;

    public UserView(User.Id userId, User user, User.Id viewer) {
        this.id = userId.getId();

        if (user != null) {
            this.username = user.getUsername();
            this.lastSeen = user.getLastSeen();
            this.avatarUrl = user.getAvatarUrl().toString();
            this.language = user.getLanguage();

            if (user.getId().equals(viewer)) {
                this.email = user.getEmail();
            }
        }
    }
}
