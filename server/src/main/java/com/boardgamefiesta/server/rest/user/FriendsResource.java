package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.domain.user.Friends;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.Data;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/friends")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
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
