package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.GWTException;
import com.wetjens.gwt.Location;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PossibleMove;
import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.LogEntries;
import com.wetjens.gwt.server.domain.LogEntry;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import com.wetjens.gwt.server.rest.view.GameView;
import com.wetjens.gwt.server.rest.view.state.PossibleBuyView;
import com.wetjens.gwt.server.rest.view.state.PossibleDeliveryView;
import com.wetjens.gwt.server.rest.view.state.PossibleMoveView;
import com.wetjens.gwt.server.rest.view.state.StateView;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
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
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
@Slf4j
public class GameResource {

    @Inject
    private Games games;

    @Inject
    private LogEntries logEntries;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/create")
    public GameView create(@NotNull @Valid CreateGameRequest request) {
        var currentUser = currentUser();

        var invitedUsers = request.getInviteUserIds().stream()
                .map(userId -> users.findOptionallyById(User.Id.of(userId))
                        .orElseThrow(() -> APIException.badRequest(APIError.NO_SUCH_USER, userId)))
                .collect(Collectors.toSet());

        Game game = Game.create(currentUser, invitedUsers, request.isBeginner());

        games.add(game);

        logEntries.addAll(Stream.concat(Stream.of(
                new LogEntry(game.getId(), currentUser.getId(), LogEntry.Type.CREATE, Collections.emptyList())),
                invitedUsers.stream()
                        .map(invitedUser -> new LogEntry(game.getId(), currentUser.getId(), LogEntry.Type.INVITE, List.of(invitedUser.getUsername()))))
                .collect(Collectors.toList()));

        var userMap = Stream.concat(Stream.of(currentUser), invitedUsers.stream())
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

        return new GameView(game, getUserMapById(game), currentUserId());
    }

    @POST
    @Path("/{id}/start")
    public GameView start(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        checkOwner(game);

        game.start();

        games.update(game);

        logEntries.add(new LogEntry(game.getId(), currentUserId(), LogEntry.Type.START, Collections.emptyList()));

        return new GameView(game, getUserMapById(game), currentUserId());
    }

    @POST
    @Path("/{id}/accept")
    public GameView accept(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.acceptInvite(currentUserId());

        games.update(game);

        logEntries.add(new LogEntry(game.getId(), currentUserId(), LogEntry.Type.ACCEPT, Collections.emptyList()));

        return new GameView(game, getUserMapById(game), currentUserId());
    }

    @POST
    @Path("/{id}/reject")
    public GameView reject(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.rejectInvite(currentUserId());

        games.update(game);

        logEntries.add(new LogEntry(game.getId(), currentUserId(), LogEntry.Type.REJECT, Collections.emptyList()));

        return new GameView(game, getUserMapById(game), currentUserId());
    }

    @POST
    @Path("/{id}/perform")
    public StateView perform(@PathParam("id") String id, ActionRequest request) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        Map<Player, User> playerUserMap = getUserMapByColor(game);

        List<LogEntry> logEntries = new LinkedList<>();
        try {
            Action action = request.toAction(game.getState());

            game.getState().addEventListener(event -> logEntries.add(new LogEntry(game.getId(), event, playerUserMap)));

            game.perform(action);
        } catch (GWTException e) {
            throw new APIException(e.getError(), e.getParams());
        }

        games.update(game);

        this.logEntries.addAll(logEntries);

        return new StateView(game, performingPlayer, playerUserMap);
    }

    @POST
    @Path("/{id}/skip")
    public StateView skip(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        try {
            game.skip();
        } catch (GWTException e) {
            throw new APIException(e.getError(), e.getParams());
        }

        games.update(game);

        logEntries.add(new LogEntry(game.getId(), currentUserId(), LogEntry.Type.SKIP, Collections.emptyList()));

        return new StateView(game, performingPlayer, getUserMapByColor(game));
    }

    @POST
    @Path("/{id}/end-turn")
    public StateView endTurn(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        try {
            game.endTurn();
        } catch (GWTException e) {
            throw new APIException(e.getError(), e.getParams());
        }

        games.update(game);

        logEntries.add(new LogEntry(game.getId(), currentUserId(), LogEntry.Type.END_TURN, Collections.emptyList()));

        return new StateView(game, performingPlayer, getUserMapByColor(game));
    }

    @GET
    @Path("/{id}/state")
    public StateView getState(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        if (game.getState() == null) {
            throw new NotFoundException();
        }

        checkViewAllowed(game);

        var viewingPlayer = determinePlayer(game);

        return new StateView(game, viewingPlayer, getUserMapByColor(game));
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

        Set<PossibleMove> possibleMoves;
        try {
            possibleMoves = game.getState().possibleMoves(game.getState().getCurrentPlayer(), to);
        } catch (GWTException e) {
            throw new APIException(e.getError(), e.getParams());
        }

        Map<Player, User> userMapByColor = getUserMapByColor(game);
        return possibleMoves.stream()
                .map(possibleMove -> new PossibleMoveView(possibleMove, userMapByColor))
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/{id}/log")
    public List<LogEntryView> getLog(@PathParam("id") String id, @QueryParam("since") String since) {
        var game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        Map<User.Id, User> userMap = getUserMapById(game);

        return logEntries.findSince(Game.Id.of(id), Instant.parse(since))
                .limit(100)
                .map(logEntry -> new LogEntryView(logEntry, userMap))
                .collect(Collectors.toList());
    }

    private void checkOwner(Game game) {
        if (!game.getOwner().equals(currentUserId())) {
            throw APIException.forbidden(APIError.MUST_BE_OWNER);
        }
    }

    private Player determinePlayer(Game game) {
        var currentUserId = currentUserId();

        return game.getPlayerByUserId(currentUserId)
                .map(com.wetjens.gwt.server.domain.Player::getColor)
                .orElseThrow(() -> APIException.forbidden(APIError.NOT_PLAYER_IN_GAME));
    }

    private void checkViewAllowed(Game game) {
        var currentUserId = currentUserId();

        if (game.getPlayers().stream().noneMatch(player -> player.getUserId().equals(currentUserId))) {
            throw APIException.forbidden(APIError.NOT_PLAYER_IN_GAME);
        }
    }

    private User.Id currentUserId() {
        if (securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("");
        }
        return User.Id.of(securityContext.getUserPrincipal().getName());
    }

    private User currentUser() {
        return users.findOptionallyById(currentUserId())
                .orElseThrow(() -> APIException.serverError(APIError.NO_SUCH_USER));
    }

    private Player checkTurn(Game game) {
        var performingPlayer = determinePlayer(game);

        if (game.getState().getCurrentPlayer() != performingPlayer) {
            log.debug("End Turn: current user {} is {}, but current player is {}", currentUserId().getId(), performingPlayer, game.getState().getCurrentPlayer());
            throw APIException.forbidden(APIError.NOT_YOUR_TURN);
        }

        return performingPlayer;
    }

    private Map<Player, User> getUserMapByColor(Game game) {
        return game.getPlayers().stream()
                .collect(Collectors.toMap(com.wetjens.gwt.server.domain.Player::getColor, player -> users.findOptionallyById(player.getUserId())
                        .orElseThrow(() -> APIException.serverError(APIError.NO_SUCH_USER, player.getUserId().getId()))));
    }

    private Map<User.Id, User> getUserMapById(Game game) {
        return game.getPlayers().stream()
                .flatMap(player -> users.findOptionallyById(player.getUserId()).stream())
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
