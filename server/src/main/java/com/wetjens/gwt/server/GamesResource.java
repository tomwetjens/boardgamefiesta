package com.wetjens.gwt.server;

import java.util.Set;

import com.wetjens.gwt.Game;
import com.wetjens.gwt.Player;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GamesResource {

    @Inject
    private GameRepository gameRepository;

    @GET
    @Path("/{id}")
    public GameView getGame(@PathParam("id") String id) {
        Game game = gameRepository.findById(id);

        // TODO Determine viewing player from authenticated user
        Player viewingPlayer = game.getCurrentPlayer();

        return new GameView(game, viewingPlayer);
    }

    @POST
    @Path("/{id}/perform")
    public GameView perform(@PathParam("id") String id, PerformRequest request) {
        Game game = gameRepository.findById(id);

        // TODO Determine viewing player from authenticated user
        Player performingPlayer = game.getCurrentPlayer();

        if (game.getCurrentPlayer() != performingPlayer) {
            throw new IllegalStateException("Not current player");
        }

        game.perform(request.toAction(game));

        gameRepository.save(id, game);

        return new GameView(game, performingPlayer);
    }

    @POST
    @Path("/{id}/end-turn")
    public GameView endTurn(@PathParam("id") String id) {
        Game game = gameRepository.findById(id);

        // TODO Determine viewing player from authenticated user
        Player performingPlayer = game.getCurrentPlayer();

        if (game.getCurrentPlayer() != performingPlayer) {
            throw new IllegalStateException("Not current player");
        }

        game.endTurn();

        gameRepository.save(id, game);

        return new GameView(game, performingPlayer);
    }

}
