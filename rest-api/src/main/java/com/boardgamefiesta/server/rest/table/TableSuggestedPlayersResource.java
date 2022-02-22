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

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.Friends;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.user.view.UserView;

import javax.annotation.security.RolesAllowed;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Path("/tables/{tableId}/suggested-players")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
public class TableSuggestedPlayersResource {

    @Inject
    Friends friends;

    @Inject
    Users users;

    @Inject
    Tables tables;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<UserView> get(@PathParam("tableId") String tableId) {
        var table = tables.findById(Table.Id.of(tableId))
                .orElseThrow(NotFoundException::new);

        var currentUserId = currentUser.getId();

        var friends = this.friends.findByUserId(currentUserId, 200)
                .map(friend -> friend.getId().getOtherUserId())
                .collect(Collectors.toSet());

        var recentlyPlayedWith = tables.findAll(currentUserId, table.getGame().getId(), 10)
                .map(Table::getPlayers).flatMap(Collection::stream)
                .filter(Player::isPlaying)
                .flatMap(player -> player.getUserId().stream())
                .distinct()
                .filter(friends::contains)
                .filter(userId -> table.getPlayerByUserId(userId).isEmpty())
                .limit(5)
                .collect(Collectors.toSet());

        return Stream.concat(recentlyPlayedWith.stream(), friends.stream().filter(friend -> !recentlyPlayedWith.contains(friend)))
                .filter(userId -> table.getPlayerByUserId(userId).isEmpty())
                .flatMap(userId -> users.findById(userId).stream())
                .map(UserView::new)
                .limit(5)
                .collect(Collectors.toList());
    }

}
