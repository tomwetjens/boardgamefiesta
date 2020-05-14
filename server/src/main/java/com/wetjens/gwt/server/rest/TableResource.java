package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.api.Options;
import com.wetjens.gwt.server.domain.APIError;
import com.wetjens.gwt.server.domain.APIException;
import com.wetjens.gwt.server.domain.Games;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.Table;
import com.wetjens.gwt.server.domain.Tables;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.domain.Users;
import com.wetjens.gwt.server.rest.view.LogEntryView;
import com.wetjens.gwt.server.rest.view.TableView;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/tables")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Slf4j
public class TableResource {

    @Inject
    private Games games;

    @Inject
    private Tables tables;

    @Inject
    private Users users;

    @Context
    private SecurityContext securityContext;

    @GET
    public List<TableView> getTables() {
        var currentUserId = currentUserId();

        return tables.findByUserId(currentUserId)
                .map(table -> new TableView(table, getUserMapById(table), currentUserId))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/create")
    @Transactional
    public TableView create(@NotNull @Valid CreateTableRequest request) {
        var currentUser = currentUser();

        var invitedUsers = request.getInviteUserIds() != null
                ? request.getInviteUserIds().stream()
                .map(userId -> users.findOptionallyById(User.Id.of(userId))
                        .orElseThrow(() -> APIException.badRequest(APIError.NO_SUCH_USER, userId)))
                .collect(Collectors.toSet())
                : Collections.<User>emptySet();

        Table table = Table.create(games.get(request.getGame()), currentUser, invitedUsers, new Options(request.getOptions() != null ? request.getOptions() : Collections.emptyMap()));

        tables.add(table);

        return new TableView(table, getUserMapById(table), currentUser.getId());
    }

    @GET
    @Path("/{id}")
    public TableView get(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkViewAllowed(table);

        return new TableView(table, getUserMapById(table), currentUserId());
    }

    @POST
    @Path("/{id}/start")
    @Transactional
    public TableView start(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        checkOwner(table);

        table.start();

        tables.update(table);

        return new TableView(table, getUserMapById(table), currentUserId());
    }

    @POST
    @Path("/{id}/accept")
    @Transactional
    public TableView accept(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.acceptInvite(currentUserId());

        tables.update(table);

        return new TableView(table, getUserMapById(table), currentUserId());
    }

    @POST
    @Path("/{id}/reject")
    @Transactional
    public TableView reject(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.rejectInvite(currentUserId());

        tables.update(table);

        return new TableView(table, getUserMapById(table), currentUserId());
    }

    @POST
    @Path("/{id}/perform")
    @Transactional
    public Object perform(@PathParam("id") String id, ActionRequest request) {
        var table = tables.findById(Table.Id.of(id));

        var performingPlayer = checkTurn(table);

        table.perform(request.toAction(table));

        tables.update(table);

        var state = table.getState().get();
        return table.getGame().toView(state, state.getPlayerByName(performingPlayer.getId().getId()));
    }

    @POST
    @Path("/{id}/skip")
    @Transactional
    public Object skip(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        var performingPlayer = checkTurn(table);

        table.skip();

        tables.update(table);

        var state = table.getState().get();
        return table.getGame().toView(state, state.getPlayerByName(performingPlayer.getId().getId()));
    }

    @POST
    @Path("/{id}/end-turn")
    @Transactional
    public Object endTurn(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        var performingPlayer = checkTurn(table);

        table.endTurn();

        tables.update(table);

        var state = table.getState().get();
        return table.getGame().toView(state, state.getPlayerByName(performingPlayer.getId().getId()));
    }

    @POST
    @Path("/{id}/propose-to-leave")
    @Transactional
    public void proposeToLeave(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.proposeToLeave(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/agree-to-leave")
    @Transactional
    public void agreeToLeave(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.agreeToLeave(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/leave")
    @Transactional
    public void leave(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.leave(currentUserId());

        tables.update(table);
    }

    @POST
    @Path("/{id}/abandon")
    @Transactional
    public void abandon(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id));

        table.abandon(currentUserId());

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

        var userMap = getUserMapById(table);

        return table.getLog().since(Instant.parse(since))
                .map(logEntry -> new LogEntryView(table, logEntry, userMap))
                .collect(Collectors.toList());
    }

    private void checkOwner(Table table) {
        if (!table.getOwner().equals(currentUserId())) {
            throw APIException.forbidden(APIError.MUST_BE_OWNER);
        }
    }

    private Player determinePlayer(Table table) {
        var currentUserId = currentUserId();

        return table.getPlayerByUserId(currentUserId);
    }

    private void checkViewAllowed(Table table) {
        var currentUserId = currentUserId();

        if (table.getPlayers().stream().noneMatch(player -> currentUserId.equals(player.getUserId()))) {
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

    private Player checkTurn(Table table) {
        var performingPlayer = determinePlayer(table);
        var currentPlayer = table.getCurrentPlayer();

        if (!currentPlayer.equals(performingPlayer)) {
            throw APIException.forbidden(APIError.NOT_YOUR_TURN);
        }

        return performingPlayer;
    }

    private Map<User.Id, User> getUserMapById(Table table) {
        return table.getPlayers().stream()
                .filter(player -> player.getUserId() != null)
                .flatMap(player -> users.findOptionallyById(player.getUserId()).stream())
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
