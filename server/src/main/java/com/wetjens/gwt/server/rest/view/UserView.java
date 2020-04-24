package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;

@Value
public class UserView {

    String id;
    String username;
    Instant lastSeen;
    String avatarUrl;
    String language;

    public UserView(User.Id userId, User user) {
        this.id = userId.getId();
        this.username = user != null ? user.getUsername() : null;
        this.lastSeen = user != null ? user.getLastSeen() : null;
        this.avatarUrl = user.getAvatarUrl().toString();
        this.language = user.getLanguage();
    }
}
