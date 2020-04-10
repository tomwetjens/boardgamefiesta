package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import com.wetjens.gwt.server.rest.view.GameView;
import com.wetjens.gwt.server.rest.view.state.PossibleBuyView;
import com.wetjens.gwt.server.rest.view.state.PossibleDeliveryView;
import com.wetjens.gwt.server.rest.view.state.PossibleMoveView;
import com.wetjens.gwt.server.rest.view.state.StateView;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/games")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class GameResource {

    @Inject
    private Games games;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/create")
    public GameView create(@NotNull @Valid CreateGameRequest request) {
        var currentUser = users.findById(currentUserId());

        var inviteUsers = request.getInviteUserIds().stream()
                .map(userId -> users.findOptionallyById(User.Id.of(userId))
                        .orElseThrow(() -> new BadRequestException("User not found: " + userId)))
                .collect(Collectors.toSet());

        Game game = Game.create(currentUser, inviteUsers);

        games.add(game);

        var userMap = Stream.concat(Stream.of(currentUser), inviteUsers.stream())
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return new GameView(game, userMap, currentUser.getId());
    }

    @GET
    public List<GameView> getGames() {
        var currentUserId = currentUserId();

        var userMap = new HashMap<User.Id, Optional<User>>();

        return games.findByUserId(currentUserId)
                .map(game -> new GameView(game, game.getPlayers().stream()
                        .flatMap(p -> userMap.computeIfAbsent(p.getUserId(), k -> users.findOptionallyById(k)).stream())
                        .collect(Collectors.toMap(User::getId, Function.identity())), currentUserId))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public GameView get(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        return new GameView(game, getUserMap(game), currentUserId());
    }

    @POST
    @Path("/{id}/start")
    public GameView start(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        checkOwner(game);

        game.start();

        games.update(game);

        return new GameView(game, getUserMap(game), currentUserId());
    }

    @POST
    @Path("/{id}/accept")
    public GameView accept(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.acceptInvite(currentUserId());

        games.update(game);

        return new GameView(game, getUserMap(game), currentUserId());
    }

    @POST
    @Path("/{id}/reject")
    public GameView reject(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.rejectInvite(currentUserId());

        games.update(game);

        return new GameView(game, getUserMap(game), currentUserId());
    }

    @POST
    @Path("/{id}/perform")
    public StateView perform(@PathParam("id") String id, ActionRequest request) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        game.perform(request.toAction(game.getState()));

        games.update(game);

        return new StateView(game.getState(), performingPlayer);
    }

    @POST
    @Path("/{id}/end-turn")
    public StateView endTurn(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        game.endTurn();

        games.update(game);

        return new StateView(game.getState(), performingPlayer);
    }

    @GET
    @Path("/{id}/state")
    public StateView getState(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        if (game.getState() == null) {
            throw new NotFoundException();
        }

        checkViewAllowed(game);

        var viewingPlayer = determinePlayer(game.getState());

        return new StateView(game.getState(), viewingPlayer);
    }

    @GET
    @Path("/{id}/state/possible-deliveries")
    public Set<PossibleDeliveryView> getPossibleDeliveries(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        if (game.getState() == null) {
            throw new NotFoundException();
        }

        var viewingPlayer = checkTurn(game);

        var playerState = game.getState().playerState(viewingPlayer);

        return playerState.possibleDeliveries(game.getState().getRailroadTrack()).stream()
                .map(PossibleDeliveryView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/{id}/state/possible-buys")
    public Set<PossibleBuyView> getPossibleBuys(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        if (game.getState() == null) {
            throw new NotFoundException();
        }

        var viewingPlayer = checkTurn(game);

        var playerState = game.getState().playerState(viewingPlayer);

        return game.getState().getCattleMarket().possibleBuys(playerState.getNumberOfCowboys(), playerState.getBalance()).stream()
                .map(PossibleBuyView::new)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/{id}/state/possible-moves")
    public Set<PossibleMoveView> getPossibleMoves(@PathParam("id") String id, @QueryParam("to") String toName) {
        var game = games.findById(Game.Id.of(id));

        if (game.getState() == null) {
            throw new NotFoundException();
        }

        var to = game.getState().getTrail().getLocation(toName);

        return game.getState().possibleMoves(game.getState().getCurrentPlayer(), to).stream()
                .map(steps -> new PossibleMoveView(game.getState().getPlayers().size(), steps))
                .collect(Collectors.toSet());
    }

    private void checkOwner(Game game) {
        if (!game.getOwner().equals(currentUserId())) {
            throw new ForbiddenException();
        }
    }

    private Player determinePlayer(com.wetjens.gwt.Game game) {
        var currentUserId = currentUserId();

        return game.getPlayers().stream()
                .filter(player -> player.getName().equals(currentUserId.getId()))
                .findAny()
                .orElseThrow(() -> new ForbiddenException("User not player in game"));
    }

    private void checkViewAllowed(Game game) {
        var currentUserId = currentUserId();

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
        var performingPlayer = determinePlayer(game.getState());

        if (game.getState().getCurrentPlayer() != performingPlayer) {
            throw new ForbiddenException("User not current player");
        }

        return performingPlayer;
    }

    private Map<User.Id, User> getUserMap(Game game) {
        return game.getPlayers().stream()
                .flatMap(player -> users.findOptionallyById(player.getUserId()).stream())
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
