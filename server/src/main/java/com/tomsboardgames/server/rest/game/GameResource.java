package com.tomsboardgames.server.rest.game;

import com.tomsboardgames.api.Game;
import com.tomsboardgames.server.domain.Games;
import com.tomsboardgames.server.rest.game.view.GameView;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/games")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GameResource {

    @Inject
    private Games games;

    @GET
    public List<GameView> getGames() {
        return games.findAll()
                .map(GameView::new)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public GameView getGame(@PathParam("id") String id) {
        return games.findById(Game.Id.of(id))
                .map(GameView::new)
                .orElseThrow(NotFoundException::new);
    }

}
