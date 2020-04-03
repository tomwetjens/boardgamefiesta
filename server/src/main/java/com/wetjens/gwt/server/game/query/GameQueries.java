package com.wetjens.gwt.server.game.query;

import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.game.domain.Games;
import com.wetjens.gwt.server.game.view.GameView;
import com.wetjens.gwt.server.game.view.PossibleBuyView;
import com.wetjens.gwt.server.game.view.PossibleDeliveryView;
import com.wetjens.gwt.server.game.view.PossibleMoveView;
import com.wetjens.gwt.server.game.view.StateView;
import com.wetjens.gwt.server.user.domain.User;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameQueries {

    @Inject
    private Games games;

    @Context
    private SecurityContext securityContext;

    @GET
    @Path("/{id}")
    public GameView get(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        return new GameView(game);
    }

    @GET
    @Path("/{id}/state")
    public StateView getState(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        Player viewingPlayer = determinePlayer(game.getState());

        return new StateView(game.getState(), viewingPlayer);
    }

    @GET
    @Path("/{id}/state/possible-deliveries")
    public Set<PossibleDeliveryView> getPossibleDeliveries(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        Player viewingPlayer = checkTurn(game);

        PlayerState playerState = game.getState().playerState(viewingPlayer);

        return playerState.possibleDeliveries(game.getState().getRailroadTrack()).stream()
                .map(PossibleDeliveryView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/{id}/state/possible-buys")
    public Set<PossibleBuyView> getPossibleBuys(@PathParam("id") String id) {
        Game game = games.findById(Game.Id.of(id));

        Player viewingPlayer = checkTurn(game);

        PlayerState playerState = game.getState().playerState(viewingPlayer);

        return game.getState().getCattleMarket().possibleBuys(playerState.getNumberOfCowboys(), playerState.getBalance()).stream()
                .map(PossibleBuyView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/{id}/state/possible-moves")
    public Set<PossibleMoveView> getPossibleMoves(@PathParam("id") String id, @NotBlank String toName) {
        Game game = games.findById(Game.Id.of(id));

        Location to = game.getState().getTrail().getLocation(toName);

        return game.getState().possibleMoves(game.getState().getCurrentPlayer(), to).stream()
                .map(steps -> new PossibleMoveView(game.getState().getPlayers().size(), steps))
                .collect(Collectors.toSet());
    }

    private Player determinePlayer(com.wetjens.gwt.Game game) {
        String userId = securityContext.getUserPrincipal().getName();

        return game.getPlayers().stream()
                .filter(player -> player.getName().equals(userId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("User " + userId + " not player in game"));
    }

    private void checkViewAllowed(Game game) {
        if (!game.getPlayers().contains(User.Id.of(securityContext.getUserPrincipal().getName()))) {
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
