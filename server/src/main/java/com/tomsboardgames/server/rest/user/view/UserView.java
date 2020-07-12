package com.boardgamefiesta.server.rest.user.view;

import com.boardgamefiesta.server.domain.User;
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
    String location;

    public UserView(User.Id userId, User user, User.Id viewer) {
        this.id = userId.getId();

        if (user != null) {
            this.username = user.getUsername();
            this.lastSeen = user.getLastSeen();
            this.avatarUrl = user.getAvatarUrl().toString();
            this.language = user.getLanguage();
            this.location = user.getLocation().orElse(null);

            if (user.getId().equals(viewer)) {
                this.email = user.getEmail();
            }
        }
    }
}
