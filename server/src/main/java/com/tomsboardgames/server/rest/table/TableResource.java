package com.tomsboardgames.server.rest.table;

import com.tomsboardgames.api.Game;
import com.tomsboardgames.api.Options;
import com.tomsboardgames.server.domain.*;
import com.tomsboardgames.server.domain.rating.Rating;
import com.tomsboardgames.server.domain.rating.Ratings;
import com.tomsboardgames.server.rest.table.command.ActionRequest;
import com.tomsboardgames.server.rest.table.command.CreateTableRequest;
import com.tomsboardgames.server.rest.table.command.InviteRequest;
import com.tomsboardgames.server.rest.table.view.LogEntryView;
import com.tomsboardgames.server.rest.table.view.TableView;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/tables")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Slf4j
@Transactional
public class TableResource {

    @Inject
    Games games;

    @Inject
    Tables tables;

    @Inject
    Users users;

    @Inject
    Ratings ratings;

    @Context
    SecurityContext securityContext;

    @GET
    public List<TableView> getTables() {
        var currentUserId = currentUserId();

        return tables.findByUserId(currentUserId)
                .filter(table -> table.getStatus() != Table.Status.ABANDONED)
                .map(table -> new TableView(table, getUserMap(table), getRatingMap(table), currentUserId))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/create")
    public TableView create(@NotNull @Valid CreateTableRequest request) {
        var currentUser = currentUser();

        var invitedUsers = request.getInviteUserIds() != null
                ? request.getInviteUserIds().stream()
                .map(userId -> users.findOptionallyById(User.Id.of(userId))
                        .orElseThrow(() -> APIException.badRequest(APIError.NO_SUCH_USER, userId)))
                .collect(Collectors.toSet())
                : Collections.<User>emptySet();

        Table table = Table.create(games.get(Game.Id.of(request.getGame())), currentUser, invitedUsers,
                new Options(request.getOptions() != null ? request.getOptions() : Collections.emptyMap()));

        tables.add(table);

        return new TableView(table, getUserMap(table), getRatingMap(table), currentUser.getId());
    }

    @GET
    @Path("/{id}")
    public TableView get(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkViewAllowed(table);

        return new TableView(table, getUserMap(table), getRatingMap(table), currentUserId());
    }

    @POST
    @Path("/{id}/start")
    @Transactional
    public TableView start(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkOwner(table);

        table.start();

        tables.update(table);

        return new TableView(table, getUserMap(table), getRatingMap(table), currentUserId());
    }

    @POST
    @Path("/{id}/accept")
    @Transactional
    public void accept(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.acceptInvite(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/reject")
    @Transactional
    public void reject(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.rejectInvite(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/perform")
    public void perform(@PathParam("id") String id, ActionRequest request) {
        var table = tables.findById(Table.Id.of(id));

        checkTurn(table);

        table.perform(request.toAction(table));

        tables.update(table);
    }

    @POST
    @Path("/{id}/skip")
    public void skip(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkTurn(table);

        table.skip();

        tables.update(table);
    }

    @POST
    @Path("/{id}/end-turn")
    public void endTurn(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkTurn(table);

        table.endTurn();

        tables.update(table);
    }

    @POST
    @Path("/{id}/propose-to-leave")
    public void proposeToLeave(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.proposeToLeave(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/agree-to-leave")
    public void agreeToLeave(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.agreeToLeave(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/leave")
    public void leave(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.leave(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/abandon")
    public void abandon(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkOwner(table);

        table.abandon();

        tables.update(table);
    }

    @POST
    @Path("/{id}/invite")
    public void invite(@PathParam("id") String id, @NotNull @Valid InviteRequest request) {
        var table = tables.findById(Table.Id.of(id));

        checkOwner(table);

        table.invite(users.findOptionallyById(User.Id.of(request.getUserId()))
                .orElseThrow(() -> APIException.badRequest(APIError.NO_SUCH_USER, request.getUserId())));

        tables.update(table);
    }

    @POST
    @Path("/{id}/add-computer")
    public void addComputer(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkOwner(table);

        table.addComputer();

        tables.update(table);
    }

    @POST
    @Path("/{id}/players/{playerId}/kick")
    public void kick(@PathParam("id") String id, @PathParam("playerId") String playerId) {
        var table = tables.findById(Table.Id.of(id));

        checkOwner(table);

        table.kick(table.getPlayerById(Player.Id.of(playerId))
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME)));

        tables.update(table);
    }

    @POST
    @Path("/{id}/change-options")
    public void changeOptions(@PathParam("id") String id, @NotNull @Valid ChangeOptionsRequest request) {
        var table = tables.findById(Table.Id.of(id));

        checkOwner(table);

        table.changeOptions(new Options(request.getOptions()));

        tables.update(table);
    }

    @GET
    @Path("/{id}/state")
    public Object getState(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkViewAllowed(table);

        var state = table.getState().get();

        if (state == null) {
            throw new NotFoundException();
        }

        var viewingPlayer = determinePlayer(table);

        return table.getGame().toView(state, state.getPlayerByName(viewingPlayer.getId().getId()));
    }

    @GET
    @Path("/{id}/log")
    public List<LogEntryView> getLog(@PathParam("id") String id, @QueryParam("since") String since) {
        var table = tables.findById(Table.Id.of(id));

        checkViewAllowed(table);

        var userMap = new HashMap<User.Id, User>();
        var ratingMap = getRatingMap(table);

        return table.getLog().since(Instant.parse(since))
                .map(logEntry -> new LogEntryView(table, logEntry,
                        userId -> userMap.computeIfAbsent(userId, this.users::findById),
                        ratingMap))
                .collect(Collectors.toList());
    }

    private void checkOwner(Table table) {
        if (!table.getOwner().equals(currentUserId())) {
            throw APIException.forbidden(APIError.MUST_BE_OWNER);
        }
    }

    private Player determinePlayer(Table table) {
        var currentUserId = currentUserId();

        return table.getPlayerByUserId(currentUserId)
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));
    }

    private void checkViewAllowed(Table table) {
        var currentUserId = currentUserId();

        if (table.getPlayers().stream().noneMatch(player -> currentUserId.equals(player.getUserId().orElse(null)))) {
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
                .orElseThrow(() -> APIException.internalError(APIError.NO_SUCH_USER));
    }

    private void checkTurn(Table table) {
        var performingPlayer = determinePlayer(table);
        var currentPlayer = table.getCurrentPlayer();

        if (!currentPlayer.equals(performingPlayer)) {
            throw APIException.forbidden(APIError.NOT_YOUR_TURN);
        }
    }

    private Map<User.Id, User> getUserMap(Table table) {
        return table.getPlayers().stream()
                .flatMap(player -> player.getUserId().flatMap(users::findOptionallyById).stream())
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<User.Id, Rating> getRatingMap(Table table) {
        return table.getPlayers().stream()
                .flatMap(player -> player.getUserId().stream())
                .map(userId -> ratings.findLatest(userId, table.getGame().getId()))
                .collect(Collectors.toMap(Rating::getUserId, Function.identity()));
    }
}
