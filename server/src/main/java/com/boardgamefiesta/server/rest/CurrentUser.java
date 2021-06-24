package com.boardgamefiesta.server.rest;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import java.util.Optional;

@RequestScoped
public class CurrentUser {

    @Inject
    Users users;

    @Inject
    JsonWebToken jsonWebToken;

    private User.Id id;
    private User user;

    public User.Id getId() {
        return getOptionalId().orElseThrow(() -> new NotAuthorizedException("User not authenticated"));
    }

    public Optional<User.Id> getOptionalId() {
        if (id == null) {
            getOptionalCognitoUsername()
                    .ifPresent(cognitoUsername -> users.findIdByCognitoUsername(cognitoUsername)
                            .ifPresentOrElse(id -> this.id = id, this::createAutomatically));
        }
        return Optional.ofNullable(id);
    }

    private void createAutomatically() {
        var user = User.createAutomatically(getCognitoUsername(), getEmail());

        // TODO Fix race condition where multiple items are created for the same username (in case of parallel requests)
        users.add(user);

        this.user = user;
        this.id = user.getId();
    }

    public User get() {
        if (user == null) {
            var id = getId();
            if (user == null) {
                user = users.findById(id)
                        .orElseThrow(() -> new NotAuthorizedException("User not found"));
            }
        }
        return user;
    }

    private String getCognitoUsername() {
        return getOptionalCognitoUsername().orElseThrow(() -> new NotAuthorizedException("Invalid token"));
    }

    private Optional<String> getOptionalCognitoUsername() {
        return Optional.ofNullable(jsonWebToken.getClaim("cognito:username"));
    }

    private String getEmail() {
        return jsonWebToken.getClaim("email");
    }

}
