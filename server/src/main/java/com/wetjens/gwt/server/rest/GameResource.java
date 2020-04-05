package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.rest.view.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/games/{id}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class GameResource {

    @Inject
    public Games games;

    @Context
    public SecurityContext securityContext;

    @GET
    public GameView get(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        return new GameView(game);
    }

    @GET
    @Path("/state")
    public StateView getState(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        Player viewingPlayer = determinePlayer(game.getState());

        return new StateView(game.getState(), viewingPlayer);
    }

    @GET
    @Path("/state/possible-deliveries")
    public Set<PossibleDeliveryView> getPossibleDeliveries(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        Player viewingPlayer = checkTurn(game);

        PlayerState playerState = game.getState().playerState(viewingPlayer);

        return playerState.possibleDeliveries(game.getState().getRailroadTrack()).stream()
                .map(PossibleDeliveryView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/state/possible-buys")
    public Set<PossibleBuyView> getPossibleBuys(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        Player viewingPlayer = checkTurn(game);

        PlayerState playerState = game.getState().playerState(viewingPlayer);

        return game.getState().getCattleMarket().possibleBuys(playerState.getNumberOfCowboys(), playerState.getBalance()).stream()
                .map(PossibleBuyView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/state/possible-moves")
    public Set<PossibleMoveView> getPossibleMoves(@PathParam("id") String id, @NotBlank String toName) {
        Game game = games.findById(Game.Id.of(id));

        Location to = game.getState().getTrail().getLocation(toName);

        return game.getState().possibleMoves(game.getState().getCurrentPlayer(), to).stream()
                .map(steps -> new PossibleMoveView(game.getState().getPlayers().size(), steps))
                .collect(Collectors.toSet());
    }

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
    public StateView perform(@PathParam("id") String id, PerformActionRequest request) {
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

    private void checkOwner(Game game) {
        if (!game.getOwner().equals(currentUserId())) {
            throw new ForbiddenException();
        }
    }

    private Player determinePlayer(com.wetjens.gwt.Game game) {
        User.Id currentUserId = currentUserId();

        return game.getPlayers().stream()
                .filter(player -> player.getName().equals(currentUserId.getId()))
                .findAny()
                .orElseThrow(() -> new ForbiddenException("User not player in game"));
    }

    private void checkViewAllowed(Game game) {
        User.Id currentUserId = currentUserId();
        if (game.getPlayers().stream().noneMatch(player -> player.getUserId().equals(currentUserId))) {
            throw new ForbiddenException("User " + currentUserId.getId() + " not player in game " + game.getId().getId());
        }
    }

    private User.Id currentUserId() {
        if (securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("User not logged in");
        }
        return User.Id.of(securityContext.getUserPrincipal().getName());
    }

    private Player checkTurn(Game game) {
        Player performingPlayer = determinePlayer(game.getState());

        if (game.getState().getCurrentPlayer() != performingPlayer) {
            throw new ForbiddenException("User not current player");
        }

        return performingPlayer;
    }
}
