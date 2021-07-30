package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.domain.featuretoggle.FeatureToggle;
import com.boardgamefiesta.domain.featuretoggle.FeatureToggles;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
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
@RolesAllowed(Roles.USER)
@Slf4j
public class GameRankingResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    FeatureToggles featureToggles;

    @Inject
    Ratings ratings;

    @Inject
    Users users;

    @GET
    public List<RankingView> getRanking(@PathParam("gameId") Game.Id gameId) {
        checkFeatureToggle(gameId);

        return ratings.findRanking(gameId, 10)
                .flatMap(ranking -> users.findById(ranking.getUserId())
                        .map(user -> new RankingView(user, ranking.getRating()))
                        .stream())
                .collect(Collectors.toList());
    }

    private void checkFeatureToggle(Game.Id gameId) {
        FeatureToggle.Id.forGameId(gameId)
                .map(featureToggles::get)
                .ifPresent(featureToggle -> featureToggle.throwIfNotContains(currentUser.getId()));
    }

}
