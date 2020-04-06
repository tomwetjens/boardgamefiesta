package com.wetjens.gwt.server.auth;

import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.Optional;

/**
 * Ensures User is automatically created in database after first log in with Identity Provider.
 */
@Provider
public class UserFilter implements ContainerRequestFilter {

    private SecurityIdentity securityIdentity;
    private Users users;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        securityIdentity = CDI.current().select(SecurityIdentity.class).get();

        if (!securityIdentity.isAnonymous()) {
            JsonWebToken jwt = (JsonWebToken) securityIdentity.getPrincipal();

            User.Id currentUserId = User.Id.of(securityIdentity.getPrincipal().getName());

            users = CDI.current().select(Users.class).get();
            Optional<User> user = users.findOptionallyById(currentUserId);

            if (user.isEmpty()) {
                createUser(jwt);
            } else {
                updateUser(user.get(), jwt);
            }
        }
    }

    private void updateUser(User user, JsonWebToken jwt) {
        // TODO Move claim names to configuration properties
        String username = jwt.getClaim("cognito:username");
        String email = jwt.getClaim("email");

        boolean changed = false;

        if (!user.getUsername().equals(username)) {
            user.changeUsername(username);
            changed = true;
        }

        if (!user.getEmail().equals(email)) {
            user.confirmEmail(jwt.getClaim("email"));
            changed = true;
        }

        if (changed) {
            users.update(user);
        }

        users.updateLastSeen(user.getId(), Instant.now());
    }

    private void createUser(JsonWebToken jwt) {
        // TODO Move claim names to configuration properties
        String username = jwt.getClaim("cognito:username");
        String email = jwt.getClaim("email");

        User user = User.createAutomatically(User.Id.of(jwt.getSubject()), username, email);

        users.add(user);
    }
}
