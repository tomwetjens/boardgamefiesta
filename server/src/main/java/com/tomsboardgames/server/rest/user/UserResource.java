package com.tomsboardgames.server.rest.user;

import com.tomsboardgames.server.domain.APIError;
import com.tomsboardgames.server.domain.APIException;
import com.tomsboardgames.server.domain.User;
import com.tomsboardgames.server.domain.Users;
import com.tomsboardgames.server.rest.user.view.UserView;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class UserResource {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_EMAIL_LENGTH = 6;
    private static final int MAX_SEARCH_RESULTS = 5;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @GET
    public List<UserView> searchUsers(@QueryParam("q") String q) {
        if (q != null && q.contains("@") && q.length() >= MIN_EMAIL_LENGTH) {
            return users.findByEmail(q)
                    .map(user -> new UserView(user.getId(), user, currentUserId()))
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (q != null && q.length() >= MIN_USERNAME_LENGTH) {
            return users.findByUsernameStartsWith(q)
                    .limit(MAX_SEARCH_RESULTS)
                    .map(user -> new UserView(user.getId(), user, currentUserId()))
                    .collect(Collectors.toList());
        } else {
            throw APIException.badRequest(APIError.MUST_SPECIFY_USERNAME_OR_EMAIL);
        }
    }

    @GET
    @Path("/{id}")
    public UserView get(@PathParam("id") String id) {
        var user = users.findById(User.Id.of(id));
        return new UserView(user.getId(), user, currentUserId());
    }

    private User.Id currentUserId() {
        if (securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("");
        }
        return User.Id.of(securityContext.getUserPrincipal().getName());
    }

}
