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

package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.domain.user.Friends;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.user.view.UserView;
import javax.enterprise.context.ApplicationScoped;
import lombok.Data;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Path("/friends")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
public class FriendsResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    Users users;

    @Inject
    Friends friends;

    @GET
    public List<UserView> get() {
        return friends.findByUserId(currentUser.getId(), 200)
                .flatMap(friend -> users.findById(friend.getId().getOtherUserId()).stream())
                .map(UserView::new)
                .collect(Collectors.toList());
    }

    @POST
    public void add(AddFriendRequest request) {
        var otherUserId = User.Id.of(request.getUserId());
        // Check it exists
        users.findById(otherUserId).orElseThrow(BadRequestException::new);

        friends.add(Friend.start(currentUser.getId(), otherUserId));
    }

    @DELETE
    @Path("/{otherUserId}")
    public void end(@PathParam("otherUserId") String otherUserId) {
        friends.findById(Friend.Id.of(currentUser.getId(), User.Id.of(otherUserId)))
                .ifPresent(friend -> {

                    friend.end();

                    friends.update(friend);
                });
    }

    @Data
    public static class AddFriendRequest {
        String userId;
    }
}
