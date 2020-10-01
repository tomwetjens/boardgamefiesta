package com.boardgamefiesta.server.rest.table;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.server.domain.*;
import com.boardgamefiesta.server.domain.rating.Rating;
import com.boardgamefiesta.server.domain.rating.Ratings;
import com.boardgamefiesta.server.rest.table.command.ActionRequest;
import com.boardgamefiesta.server.rest.table.command.CreateTableRequest;
import com.boardgamefiesta.server.rest.table.command.InviteRequest;
import com.boardgamefiesta.server.rest.table.view.LogEntryView;
import com.boardgamefiesta.server.rest.table.view.TableView;
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
import java.util.*;
import java.util.function.Consumer;
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

        return tables.findActive(currentUserId)
                .map(table -> new TableView(table, getUserMap(table), getRatingMap(table), currentUserId))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/create")
    public TableView create(@NotNull @Valid CreateTableRequest request) {
        var currentUser = currentUser();

        Table table = Table.create(
                games.get(Game.Id.of(request.getGame())),
                request.getMode(),
                currentUser,
                new Options(request.getOptions() != null ? request.getOptions() : Collections.emptyMap()));

        tables.add(table);

        return new TableView(table, getUserMap(table), getRatingMap(table), currentUser.getId());
    }

    @GET
    @Path("/{id}")
    public TableView get(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id), false);

        checkViewAllowed(table);

        return new TableView(table, getUserMap(table), getRatingMap(table), currentUserId());
    }

    @POST
    @Path("/{id}/start")
    @Transactional
    public TableView start(@PathParam("id") String id) {
        var result = handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.start();
        });
        return new TableView(result, getUserMap(result), getRatingMap(result), currentUserId());
    }

    @POST
    @Path("/{id}/accept")
    @Transactional
    public void accept(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.acceptInvite(currentUserId()));
    }

    @POST
    @Path("/{id}/reject")
    @Transactional
    public void reject(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.rejectInvite(currentUserId()));
    }

    @POST
    @Path("/{id}/perform")
    public void perform(@PathParam("id") String id, ActionRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkTurn(table);

            table.perform(request.toAction(table.getGame(), table.getState()));
        });
    }

    @POST
    @Path("/{id}/skip")
    public void skip(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkTurn(table);

            table.skip();
        });
    }

    @POST
    @Path("/{id}/end-turn")
    public void endTurn(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkTurn(table);

            table.endTurn();
        });
    }

    @POST
    @Path("/{id}/undo")
    public void undo(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkTurn(table);

            table.undo();
        });
    }

    @POST
    @Path("/{id}/propose-to-leave")
    public void proposeToLeave(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.proposeToLeave(currentUserId()));
    }

    @POST
    @Path("/{id}/agree-to-leave")
    public void agreeToLeave(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.agreeToLeave(currentUserId()));
    }

    @POST
    @Path("/{id}/leave")
    public void leave(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.leave(currentUserId()));
    }

    @POST
    @Path("/{id}/abandon")
    public void abandon(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.abandon();
        });
    }

    @POST
    @Path("/{id}/invite")
    public void invite(@PathParam("id") String id, @NotNull @Valid InviteRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.invite(users.findOptionallyById(User.Id.of(request.getUserId()))
                    .orElseThrow(() -> APIException.badRequest(APIError.NO_SUCH_USER)));
        });
    }

    @POST
    @Path("/{id}/add-computer")
    public void addComputer(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.addComputer();
        });
    }

    @POST
    @Path("/{id}/players/{playerId}/kick")
    public void kick(@PathParam("id") String id, @PathParam("playerId") String playerId) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.kick(table.getPlayerById(Player.Id.of(playerId))
                    .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME)));
        });
    }

    @POST
    @Path("/{id}/change-options")
    public void changeOptions(@PathParam("id") String id, @NotNull @Valid ChangeOptionsRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.changeOptions(new Options(request.getOptions()));
        });
    }

    @GET
    @Path("/{id}/state")
    public Object getState(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id), false);

        checkViewAllowed(table);

        var state = table.getState();

        if (state == null) {
            throw new NotFoundException();
        }

        var viewingPlayer = determinePlayer(table);

        return table.getGame().getProvider().getViewMapper().toView(state, viewingPlayer
                .map(Player::getId)
                .map(Player.Id::getId)
                .flatMap(state::getPlayerByName)
                .orElse(null));
    }

    @GET
    @Path("/{id}/log")
    public List<LogEntryView> getLog(@PathParam("id") String id, @QueryParam("since") String since) {
        var table = tables.findById(Table.Id.of(id), false);

        checkViewAllowed(table);

        var userMap = new HashMap<User.Id, User>();
        var ratingMap = getRatingMap(table);

        return table.getLog().since(Instant.parse(since))
                .map(logEntry -> new LogEntryView(table, logEntry,
                        userId -> userMap.computeIfAbsent(userId, k -> this.users.findOptionallyById(userId).orElse(null)),
                        ratingMap))
                .collect(Collectors.toList());
    }

    private Table handleConcurrentModification(Table.Id id, Consumer<Table> modifier) {
        int retries = 0;
        do {
            var table = tables.findById(id, true);

            modifier.accept(table);

            try {
                tables.update(table);

                return table;
            } catch (Tables.TableConcurrentlyModifiedException e) {
                if (retries >= 1) {
                    throw APIException.conflict(APIError.CONCURRENT_MODIFICATION);
                }
                retries++;
            }
        } while (true);
    }

    private void checkOwner(Table table) {
        if (!table.getOwnerId().equals(currentUserId())) {
            throw APIException.forbidden(APIError.MUST_BE_OWNER);
        }
    }

    private Optional<Player> determinePlayer(Table table) {
        var currentUserId = currentUserId();

        return table.getPlayerByUserId(currentUserId);
    }

    private void checkViewAllowed(Table table) {
//        var currentUserId = currentUserId();

//        if (table.getPlayers().stream().noneMatch(player -> currentUserId.equals(player.getUserId().orElse(null)))) {
//            throw APIException.forbidden(APIError.NOT_PLAYER_IN_GAME);
//        }
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

        if (currentPlayer == null || !currentPlayer.equals(performingPlayer.orElse(null))) {
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
