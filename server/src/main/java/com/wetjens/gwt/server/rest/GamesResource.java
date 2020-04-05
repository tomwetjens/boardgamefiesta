package com.wetjens.gwt.server.rest;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class GamesResource {

    @Inject
    private Games games;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/create")
    public void create(@NotNull @Valid CreateGameRequest request) {
        User user = users.get(currentUserId());

        Set<User> inviteUsers = request.getInviteUserIds().stream()
                .map(userId -> users.findById(User.Id.of(userId)))
                .collect(Collectors.toSet());

        games.add(Game.create(user, inviteUsers));
    }

    public User.Id currentUserId() {
        return User.Id.of(securityContext.getUserPrincipal().getName());
    }

}
