package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.server.domain.Game;
import com.boardgamefiesta.server.domain.Games;
import com.boardgamefiesta.server.rest.game.view.GameView;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/games")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GameResource {

    @Inject
    private Games games;

    @GET
    @Path("/{id}")
    public GameView getGame(@PathParam("id") String id) {
        return games.findById(Game.Id.of(id))
                .map(GameView::new)
                .orElseThrow(NotFoundException::new);
    }

}
