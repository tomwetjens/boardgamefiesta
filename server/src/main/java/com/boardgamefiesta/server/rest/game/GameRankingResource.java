package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.domain.rating.Ratings;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/games/{gameId}/ranking")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Slf4j
public class GameRankingResource {

    @Inject
    Ratings ratings;

    @Inject
    Users users;

    @GET
    public List<RankingView> getRanking(@PathParam("gameId") String gameId) {
        return ratings.findRanking(Game.Id.of(gameId), 10)
                .flatMap(ranking -> users.findById(ranking.getUserId())
                        .map(user -> new RankingView(user, ranking.getRating()))
                        .stream())
                .collect(Collectors.toList());
    }

}
