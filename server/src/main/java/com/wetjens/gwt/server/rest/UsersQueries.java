package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.server.domain.APIError;
import com.wetjens.gwt.server.domain.APIException;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import com.wetjens.gwt.server.rest.view.UserView;

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
public class UsersQueries {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_SEARCH_RESULTS = 5;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @GET
    public List<UserView> searchUsers(@QueryParam("username") String username, @QueryParam("email") String email) {
        if (email != null && !"".equals(email)) {
            return users.findByEmail(username)
                    .map(user -> new UserView(user.getId(), user))
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (username != null && username.length() >= MIN_USERNAME_LENGTH) {
            return users.findByUsernameStartsWith(username)
                    .limit(MAX_SEARCH_RESULTS)
                    .map(user -> new UserView(user.getId(), user))
                    .collect(Collectors.toList());
        } else {
            throw APIException.badRequest(APIError.MUST_SPECIFY_USERNAME_OR_EMAIL);
        }
    }

    @GET
    @Path("/{id}")
    public UserView get(@PathParam("id") String id) {
        var user = users.findById(User.Id.of(id));
        return new UserView(user.getId(), user);
    }

}
