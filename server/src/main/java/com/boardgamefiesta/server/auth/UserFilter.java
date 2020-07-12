package com.boardgamefiesta.server.auth;

import com.boardgamefiesta.server.domain.User;
import com.boardgamefiesta.server.domain.Users;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.time.Instant;

/**
 * Ensures User is automatically created in database after first log in with Identity Provider.
 */
@Provider
public class UserFilter implements ContainerRequestFilter {

    private Users users;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var securityIdentity = CDI.current().select(SecurityIdentity.class).get();

        if (!securityIdentity.isAnonymous()) {
            var jwt = (JsonWebToken) securityIdentity.getPrincipal();

            var currentUserId = User.Id.of(securityIdentity.getPrincipal().getName());

            users = CDI.current().select(Users.class).get();
            var user = users.findOptionallyById(currentUserId);

            if (user.isEmpty()) {
                createUser(jwt);
            } else {
                updateUser(user.get(), jwt);
            }
        }
    }

    private void updateUser(User user, JsonWebToken jwt) {
        // TODO Move claim names to configuration properties
        var username = (String) jwt.getClaim("cognito:username");
        var email = (String) jwt.getClaim("email");

        boolean changed = false;

        if (!user.getUsername().equals(username)) {
            user.changeUsername(username);
            changed = true;
        }

        if (!user.getEmail().equals(email)) {
            user.confirmEmail(jwt.getClaim("email"));
            changed = true;
        }

        user.lastSeen(Instant.now());

        if (changed) {
            users.update(user);
        } else {
            users.updateLastSeen(user);
        }
    }

    private void createUser(JsonWebToken jwt) {
        // TODO Move claim names to configuration properties
        var username = (String) jwt.getClaim("cognito:username");
        var email = (String) jwt.getClaim("email");

        User user = User.createAutomatically(User.Id.of(jwt.getSubject()), username, email);

        users.add(user);
    }
}
