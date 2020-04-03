package com.wetjens.gwt.server.game.command;

import java.util.Set;
import java.util.stream.Collectors;
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

import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.game.domain.Games;
import com.wetjens.gwt.server.user.domain.User;
import com.wetjens.gwt.server.user.domain.Users;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GamesCommands {

    @Inject
    private Games games;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/create")
    public void create(@NotNull @Valid CreateGameCommand command) {
        User user = users.findById(securityContext.getUserPrincipal().getName());

        Set<User> inviteUsers = command.getInviteUserIds().stream()
                .map(userId -> users.findById(userId))
                .collect(Collectors.toSet());

        games.add(Game.create(user, inviteUsers));
    }

}
