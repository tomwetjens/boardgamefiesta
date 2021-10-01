/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.server.rest.table;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.exception.APIError;
import com.boardgamefiesta.server.rest.exception.APIException;
import com.boardgamefiesta.server.rest.table.command.*;
import com.boardgamefiesta.server.rest.table.view.LogEntryView;
import com.boardgamefiesta.server.rest.table.view.TableView;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/tables")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
@Slf4j
public class TableResource {

    @Inject
    Games games;

    @Inject
    Tables tables;

    @Inject
    Users users;

    @Inject
    Ratings ratings;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<TableView> getTables() {
        var currentUserId = currentUser.getId();

        return tables.findActive(currentUserId)
                .map(table -> new TableView(table, getUserMap(table), getRatingMap(table), currentUserId))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/create")
    @Transactional
    public TableView create(@NotNull @Valid CreateTableRequest request) {
        var gameId = Game.Id.of(request.getGame());

        var game = games.get(gameId);

        Table table = Table.create(
                game,
                request.getType(),
                request.getMode(),
                currentUser.get(),
                new Options(request.getOptions() != null ? request.getOptions() : Collections.emptyMap()));

        tables.add(table);

        return new TableView(table, getUserMap(table), getRatingMap(table), currentUser.getId());
    }

    @GET
    @Path("/{id}")
    public TableView get(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id))
                .orElseThrow(NotFoundException::new);

        return new TableView(table, getUserMap(table), getRatingMap(table), currentUser.getId());
    }

    @POST
    @Path("/{id}/start")
    @Transactional
    public TableView start(@PathParam("id") String id) {
        var result = handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.start();
        });
        return new TableView(result, getUserMap(result), getRatingMap(result), currentUser.getId());
    }

    @POST
    @Path("/{id}/accept")
    @Transactional
    public void accept(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.acceptInvite(currentUser.get()));
    }

    @POST
    @Path("/{id}/reject")
    @Transactional
    public void reject(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.rejectInvite(currentUser.getId()));
    }

    @POST
    @Path("/{id}/join")
    @Transactional
    public void join(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.join(currentUser.get()));
    }

    @POST
    @Path("/{id}/public")
    @Transactional
    public void makePublic(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.makePublic();
        });
    }

    @POST
    @Path("/{id}/private")
    @Transactional
    public void makePrivate(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.makePrivate();
        });
    }

    @POST
    @Path("/{id}/perform")
    @Transactional
    public void perform(@PathParam("id") String id, ActionRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            var player = determinePlayer(table);
            checkTurn(table, player);

            table.perform(player, request.toAction(table.getGame(), table.getState()));
        });
    }

    @POST
    @Path("/{id}/skip")
    @Transactional
    public void skip(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            var player = determinePlayer(table);
            checkTurn(table, player);

            table.skip(player);
        });
    }

    @POST
    @Path("/{id}/end-turn")
    @Transactional
    public void endTurn(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            var player = determinePlayer(table);
            checkTurn(table, player);

            table.endTurn(player);
        });
    }

    @POST
    @Path("/{id}/undo")
    @Transactional
    public void undo(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            var player = determinePlayer(table);
            checkTurn(table, player);

            table.undo(player);
        });
    }

    @POST
    @Path("/{id}/propose-to-leave")
    @Transactional
    public void proposeToLeave(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.proposeToLeave(currentUser.getId()));
    }

    @POST
    @Path("/{id}/agree-to-leave")
    @Transactional
    public void agreeToLeave(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.agreeToLeave(currentUser.getId()));
    }

    @POST
    @Path("/{id}/leave")
    @Transactional
    public void leave(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table ->
                table.leave(currentUser.getId()));
    }

    @POST
    @Path("/{id}/abandon")
    @Transactional
    public void abandon(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.abandon();
        });
    }

    @POST
    @Path("/{id}/invite")
    @Transactional
    public void invite(@PathParam("id") String id, @NotNull @Valid InviteRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.invite(users.findById(User.Id.of(request.getUserId()))
                    .orElseThrow(() -> APIException.badRequest(APIError.NO_SUCH_USER)));
        });
    }

    @POST
    @Path("/{id}/add-computer")
    @Transactional
    public void addComputer(@PathParam("id") String id) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.addComputer();
        });
    }

    @POST
    @Path("/{id}/players/{playerId}/kick")
    @Transactional
    public void kick(@PathParam("id") String id, @PathParam("playerId") String playerId) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            if (table.getStatus() == Table.Status.NEW) {
                checkOwner(table);
            }
            table.kick(currentUser.getId(), table.getPlayerById(Player.Id.of(playerId))
                    .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME)));
        });
    }

    @POST
    @Path("/{id}/players/{playerId}/force-end-turn")
    @Transactional
    public void forceEndTurn(@PathParam("id") String id, @PathParam("playerId") String playerId) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            table.forceEndTurn(currentUser.getId(), table.getPlayerById(Player.Id.of(playerId))
                    .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME)));
        });
    }

    @POST
    @Path("/{id}/change-options")
    @Transactional
    public void changeOptions(@PathParam("id") String id, @NotNull @Valid ChangeOptionsRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.changeOptions(new Options(request.getOptions()));
        });
    }

    @POST
    @Path("/{id}/change-type")
    @Transactional
    public void changeType(@PathParam("id") String id, @NotNull @Valid ChangeTypeRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.changeType(request.getType());
        });
    }

    @POST
    @Path("/{id}/change-mode")
    @Transactional
    public void changeType(@PathParam("id") String id, @NotNull @Valid ChangeModeRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.changeMode(request.getMode());
        });
    }

    @POST
    @Path("/{id}/change-min-max-players")
    @Transactional
    public void changeMinMaxPlayers(@PathParam("id") String id, @NotNull @Valid ChangeMinMaxPlayersRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.changeMinMaxNumberOfPlayers(request.getMinNumberOfPlayers(), request.getMaxNumberOfPlayers());
        });
    }

    @POST
    @Path("/{id}/change-auto-start")
    @Transactional
    public void changeAutoStart(@PathParam("id") String id, @NotNull @Valid ChangeAutoStartRequest request) {
        handleConcurrentModification(Table.Id.of(id), table -> {
            checkOwner(table);

            table.changeAutoStart(request.isAutoStart());
        });
    }

    @GET
    @Path("/{id}/state")
    public Object getState(@PathParam("id") String id) {
        var table = tables.findById(Table.Id.of(id))
                .orElseThrow(NotFoundException::new);

        var state = table.getState();

        if (state == null) {
            throw new NotFoundException();
        }

        var viewingPlayer = determineViewingPlayer(table);

        return table.getGame().getProvider().getViewMapper().toView(state, viewingPlayer
                .map(Player::getId)
                .map(Player.Id::getId)
                .flatMap(state::getPlayerByName)
                .orElse(null));
    }

    @GET
    @Path("/{id}/log")
    public List<LogEntryView> getLog(@PathParam("id") String id,
                                     @QueryParam("since") String since,
                                     @QueryParam("before") String before,
                                     @QueryParam("limit") Integer requestedLimit) {
        var tableId = Table.Id.of(id);

        var userMap = new HashMap<User.Id, User>();

        var limit = requestedLimit != null ? requestedLimit : 2000;

        if (since == null && before == null) {
            throw new BadRequestException();
        }

        return tables.findLogEntries(tableId,
                since != null ? Instant.parse(since) : Instant.ofEpochSecond(0),
                before != null ? Instant.parse(before) : Instant.now(), limit)
                .map(logEntry -> new LogEntryView(logEntry,
                        userId -> userMap.computeIfAbsent(userId, k -> this.users.findById(userId).orElse(null))))
                .collect(Collectors.toList());
    }

    private Table handleConcurrentModification(Table.Id id, Consumer<Table> modifier) {
        int retries = 0;
        do {
            var table = tables.findById(id)
                    .orElseThrow(NotFoundException::new);

            modifier.accept(table);

            try {
                tables.update(table);

                return table;
            } catch (ConcurrentModificationException e) {
                if (retries >= 1) {
                    throw e;
                }
                retries++;
            }
        } while (true);
    }

    private void checkOwner(Table table) {
        if (!table.getOwnerId().equals(currentUser.getId())) {
            throw APIException.forbidden(APIError.MUST_BE_OWNER);
        }
    }

    private Player determinePlayer(Table table) {
        var currentUserId = currentUser.getId();

        return table.getPlayerByUserId(currentUserId).orElseThrow(() -> APIException.forbidden(APIError.NOT_PLAYER_IN_GAME));
    }

    private Optional<Player> determineViewingPlayer(Table table) {
        var currentUserId = currentUser.getId();

        return table.getPlayerByUserId(currentUserId);
    }

    private void checkTurn(Table table, Player player) {
        var currentPlayer = table.getCurrentPlayers();

        if (currentPlayer == null || !currentPlayer.contains(player)) {
            throw APIException.forbidden(APIError.NOT_YOUR_TURN);
        }
    }

    private Map<User.Id, User> getUserMap(Table table) {
        return table.getPlayers().stream()
                .flatMap(player -> player.getUserId().flatMap(users::findById).stream())
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<User.Id, Rating> getRatingMap(Table table) {
        return table.getPlayers().stream()
                .flatMap(player -> player.getUserId().stream())
                .map(userId -> ratings.findLatest(userId, table.getGame().getId(), Instant.now()))
                .collect(Collectors.toMap(Rating::getUserId, Function.identity()));
    }
}
