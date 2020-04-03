package com.wetjens.gwt.server.game.command;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.game.domain.Games;
import com.wetjens.gwt.server.game.view.GameView;
import com.wetjens.gwt.server.game.view.PerformRequest;
import com.wetjens.gwt.server.game.view.StateView;

@Path("/games/{id}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameCommands {

    @Inject
    private Games games;

    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/start")
    public GameView start(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        checkOwner(game);

        game.start();

        games.update(game);

        return new GameView(game);
    }

    @POST
    @Path("/perform")
    public StateView perform(@PathParam("id") String id, PerformRequest request) {
        Game game = games.findById(Game.Id.of(id));

        Player performingPlayer = checkTurn(game);

        game.perform(request.toAction(game.getState()));

        games.update(game);

        return new StateView(game.getState(), performingPlayer);
    }

    @POST
    @Path("/end-turn")
    public StateView endTurn(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        Player performingPlayer = checkTurn(game);

        game.getState().endTurn();

        games.update(game);

        return new StateView(game.getState(), performingPlayer);
    }

    private Player determinePlayer(com.wetjens.gwt.Game game) {
        String userId = securityContext.getUserPrincipal().getName();

        return game.getPlayers().stream()
                .filter(player -> player.getName().equals(userId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("User " + userId + " not player in game"));
    }

    private void checkOwner(Game game) {
        if (!game.getOwner().getId().equals(securityContext.getUserPrincipal().getName())) {
            throw new ForbiddenException();
        }
    }

    private Player checkTurn(Game game) {
        Player performingPlayer = determinePlayer(game.getState());

        if (game.getState().getCurrentPlayer() != performingPlayer) {
            throw new IllegalStateException("Not current player");
        }
        return performingPlayer;
    }
}
