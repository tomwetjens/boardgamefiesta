package com.wetjens.gwt.server.rest.game;

import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.rest.game.view.GameView;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/games")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Slf4j
public class GameResource {

    @Inject
    private Games games;

    @GET
    public List<GameView> getGames() {
        return games.findAll()
                .map(GameView::new)
                .collect(Collectors.toList());
    }
}
