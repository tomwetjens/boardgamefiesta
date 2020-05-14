package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.Implementations;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import com.wetjens.gwt.server.rest.view.GameView;
import com.wetjens.gwt.server.rest.view.LogEntryView;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/games")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Slf4j
public class GameResource {

    @Inject
    private Implementations implementations;

    @Inject
    private Games games;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @GET
    public List<GameView> getGames() {
        var currentUserId = currentUserId();

        return games.findByUserId(currentUserId)
                .map(game -> new GameView(game, getUserMapById(game), currentUserId))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/create")
    @Transactional
    public GameView create(@NotNull @Valid CreateGameRequest request) {
        var currentUser = currentUser();

        var invitedUsers = request.getInviteUserIds().stream()
                .map(userId -> users.findOptionallyById(User.Id.of(userId))
                        .orElseThrow(() -> APIException.badRequest(APIError.NO_SUCH_USER, userId)))
                .collect(Collectors.toSet());

        Game game = Game.create(implementations.get(request.getName()), currentUser, invitedUsers, request.getOptions());

        games.add(game);

        return new GameView(game, getUserMapById(game), currentUser.getId());
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
    @Transactional
    public GameView start(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        checkOwner(game);

        game.start();

        games.update(game);

        return new GameView(game, getUserMapById(game), currentUserId());
    }

    @POST
    @Path("/{id}/accept")
    @Transactional
    public GameView accept(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.acceptInvite(currentUserId());

        games.update(game);

        return new GameView(game, getUserMapById(game), currentUserId());
    }

    @POST
    @Path("/{id}/reject")
    @Transactional
    public GameView reject(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.rejectInvite(currentUserId());

        games.update(game);

        return new GameView(game, getUserMapById(game), currentUserId());
    }

    @POST
    @Path("/{id}/perform")
    @Transactional
    public Object perform(@PathParam("id") String id, ActionRequest request) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        game.perform(request.toAction(game));

        games.update(game);

        var state = game.getState().get();
        return game.getImplementation().toView(state, state.getPlayerByName(performingPlayer.getId().getId()));
    }

    @POST
    @Path("/{id}/skip")
    @Transactional
    public Object skip(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        game.skip();

        games.update(game);

        var state = game.getState().get();
        return game.getImplementation().toView(state, state.getPlayerByName(performingPlayer.getId().getId()));
    }

    @POST
    @Path("/{id}/end-turn")
    @Transactional
    public Object endTurn(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        var performingPlayer = checkTurn(game);

        game.endTurn();

        games.update(game);

        var state = game.getState().get();
        return game.getImplementation().toView(state, state.getPlayerByName(performingPlayer.getId().getId()));
    }

    @POST
    @Path("/{id}/propose-to-leave")
    @Transactional
    public void proposeToLeave(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.proposeToLeave(currentUserId());

        games.update(game);
    }

    @POST
    @Path("/{id}/agree-to-leave")
    @Transactional
    public void agreeToLeave(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.agreeToLeave(currentUserId());

        games.update(game);
    }

    @POST
    @Path("/{id}/leave")
    @Transactional
    public void leave(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.leave(currentUserId());

        games.update(game);
    }

    @POST
    @Path("/{id}/abandon")
    @Transactional
    public void abandon(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        game.abandon(currentUserId());

        games.update(game);
    }

    @GET
    @Path("/{id}/state")
    public Object getState(@PathParam("id") String id) {
        var game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        if (game.getState() == null) {
            throw new NotFoundException();
        }

        var viewingPlayer = determinePlayer(game);

        var state = game.getState().get();
        return game.getImplementation().toView(state, state.getPlayerByName(viewingPlayer.getId().getId()));
    }

    @GET
    @Path("/{id}/log")
    public List<LogEntryView> getLog(@PathParam("id") String id, @QueryParam("since") String since) {
        var game = games.findById(Game.Id.of(id));

        checkViewAllowed(game);

        var userMap = getUserMapById(game);

        return game.getLog().since(Instant.parse(since))
                .map(logEntry -> new LogEntryView(game, logEntry, userMap))
                .collect(Collectors.toList());
    }

    private void checkOwner(Game game) {
        if (!game.getOwner().equals(currentUserId())) {
            throw APIException.forbidden(APIError.MUST_BE_OWNER);
        }
    }

    private Player determinePlayer(Game game) {
        var currentUserId = currentUserId();

        return game.getPlayerByUserId(currentUserId);
    }

    private void checkViewAllowed(Game game) {
        var currentUserId = currentUserId();

        if (game.getPlayers().stream().noneMatch(player -> currentUserId.equals(player.getUserId()))) {
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
        var currentPlayer = game.getCurrentPlayer();

        if (!currentPlayer.equals(performingPlayer)) {
            throw APIException.forbidden(APIError.NOT_YOUR_TURN);
        }

        return performingPlayer;
    }

    private Map<User.Id, User> getUserMapById(Game game) {
        return game.getPlayers().stream()
                .filter(player -> player.getUserId() != null)
                .flatMap(player -> users.findOptionallyById(player.getUserId()).stream())
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
