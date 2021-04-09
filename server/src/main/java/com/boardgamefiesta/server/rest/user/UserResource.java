package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.user.view.UserView;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
public class UserResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    Users users;

    @GET
    public UserView get() {
        var user = currentUser.get();
        return new UserView(user.getId(), user, user.getId());
    }

    @POST
    @Path("/change-username")
    public void changeUsername(ChangeUsernameRequest request) {
        var user = currentUser.get();

        user.changeUsername(request.getUsername());

        users.update(user);
    }

}
