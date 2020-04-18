package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;

@Value
public class UserView {

    private final String id;
    private final String username;
    private final Instant lastSeen;
    private final String avatarUrl;

    public UserView(User.Id userId, User user) {
        this.id = userId.getId();
        this.username = user != null ? user.getUsername() : null;
        this.lastSeen = user != null ? user.getLastSeen() : null;
        this.avatarUrl = user.getAvatarUrl().toString();
    }
}
