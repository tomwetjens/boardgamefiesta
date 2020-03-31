package com.wetjens.gwt.server;

import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.wetjens.gwt.Game;
import com.wetjens.gwt.Location;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerState;

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
        Player viewingPlayer = determinePlayer(game);

        return new GameView(game, viewingPlayer);
    }

    @POST
    @Path("/{id}/perform")
    public GameView perform(@PathParam("id") String id, PerformRequest request) {
        Game game = gameRepository.findById(id);

        Player performingPlayer = determinePlayer(game);

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

        Player performingPlayer = determinePlayer(game);

        if (game.getCurrentPlayer() != performingPlayer) {
            throw new IllegalStateException("Not current player");
        }

        game.endTurn();

        gameRepository.save(id, game);

        return new GameView(game, performingPlayer);
    }

    @GET
    @Path("/{id}/possible-deliveries")
    public Set<PossibleDeliveryView> getPossibleDeliveries(@PathParam("id") String id) {
        Game game = gameRepository.findById(id);

        Player viewingPlayer = determinePlayer(game);

        PlayerState playerState = game.playerState(viewingPlayer);

        return playerState.possibleDeliveries(game.getRailroadTrack()).stream()
                .map(PossibleDeliveryView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/{id}/possible-buys")
    public Set<PossibleBuyView> getPossibleBuys(@PathParam("id") String id) {
        Game game = gameRepository.findById(id);

        Player viewingPlayer = determinePlayer(game);

        PlayerState playerState = game.playerState(viewingPlayer);

        return game.getCattleMarket().possibleBuys(playerState.getNumberOfCowboys(), playerState.getBalance()).stream()
                .map(PossibleBuyView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/{id}/possible-moves")
    public Set<PossibleMoveView> getPossibleMoves(@PathParam("id") String id, @NotBlank String toName) {
        Game game = gameRepository.findById(id);

        Location to = game.getTrail().getLocation(toName);

        return game.possibleMoves(game.getCurrentPlayer(), to).stream()
                .map(steps -> new PossibleMoveView(game.getPlayers().size(), steps))
                .collect(Collectors.toSet());
    }

    private Player determinePlayer(Game game) {
        // TODO Determine viewing player from authenticated user
        return game.getCurrentPlayer();
    }
}
