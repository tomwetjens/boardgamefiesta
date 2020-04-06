package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.User;
import lombok.Value;

@Value
public class UserView {

    private final String id;
    private final String username;

    public UserView(User user) {
        this.id = user.getId().getId();
        this.username = user.getUsername();
    }
}
