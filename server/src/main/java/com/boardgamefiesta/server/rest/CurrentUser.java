package com.boardgamefiesta.server.rest;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;

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

    private User.Id id;
    private User user;

    public User.Id getId() {
        if (id == null) {
            var principalName = currentPrincipalName();

            id = users.findIdByCognitoUsername(principalName)
                    .orElseThrow(() -> new NotAuthorizedException("User not found"));
        }
        return id;
    }

    public User get() {
        if (user == null) {
            user = users.findById(getId())
                    .orElseThrow(() -> new NotAuthorizedException("User not found"));
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
