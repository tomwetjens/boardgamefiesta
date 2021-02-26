package com.boardgamefiesta.server.rest;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;

@RequestScoped
public class CurrentUser {

    @Inject
    Users users;

    @Inject
    HttpServletRequest request;

    private User user;

    public User.Id getId() {
        return get().getId();
    }

    public User get() {
        if (user == null) {
            var principalName = currentPrincipalName();

            user = users.findByCognitoUsername(principalName)
                    .orElseThrow(() -> new RuntimeException("User '" + principalName + "' not found"));
        }
        return user;
    }

    private String currentPrincipalName() {
        if (request.getUserPrincipal() == null || request.getUserPrincipal().getName() == null) {
            throw new NotAuthorizedException("User not authenticated");
        }
        return request.getUserPrincipal().getName();
    }

}
