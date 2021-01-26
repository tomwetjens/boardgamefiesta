package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.server.domain.user.Friend;
import com.boardgamefiesta.server.domain.user.Friends;
import com.boardgamefiesta.server.domain.user.User;
import com.boardgamefiesta.server.domain.user.Users;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.Data;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.stream.Collectors;

@Path("/friends")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class FriendsResource {

    @Inject
    Users users;

    @Inject
    Friends friends;

    @Context
    SecurityContext securityContext;

    @GET
    public List<UserView> get() {
        return friends.findByUserId(currentUserId(), 200)
                .flatMap(friend -> users.findOptionallyById(friend.getId().getOtherUserId()).stream())
                .map(UserView::new)
                .collect(Collectors.toList());
    }

    @POST
    public void add(AddFriendRequest request) {
        var user = users.findById(currentUserId(), false);
        var otherUser = users.findById(User.Id.of(request.getUserId()), false);

        friends.add(Friend.start(user, otherUser));
    }

    @DELETE
    @Path("/{otherUserId}")
    public void end(@PathParam("otherUserId") String otherUserId) {
        var friend = friends.findById(Friend.Id.of(currentUserId(), User.Id.of(otherUserId)));

        friend.end();

        friends.update(friend);
    }

    private User.Id currentUserId() {
        if (securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("");
        }
        return User.Id.of(securityContext.getUserPrincipal().getName());
    }

    @Data
    public static class AddFriendRequest {
        String userId;
    }
}
