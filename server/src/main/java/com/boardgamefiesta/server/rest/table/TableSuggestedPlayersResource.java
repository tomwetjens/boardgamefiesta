package com.boardgamefiesta.server.rest.table;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.Friends;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.user.view.UserView;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/tables/{tableId}/suggested-players")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
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

        var recentlyPlayedWith = tables.findRecent(currentUserId, table.getGame().getId(), 10)
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
