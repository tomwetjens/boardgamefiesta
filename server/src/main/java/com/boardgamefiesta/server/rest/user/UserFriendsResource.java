package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.server.domain.user.Friends;
import com.boardgamefiesta.server.domain.user.User;
import com.boardgamefiesta.server.domain.user.Users;
import com.boardgamefiesta.server.rest.user.view.UserView;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/users/{userId}/friends")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class UserFriendsResource {

    @Inject
    private Friends friends;

    @Inject
    private Users users;

    @GET
    public List<UserView> get(@PathParam("userId") String userId) {
        return friends.findByUserId(User.Id.of(userId), 200)
                .flatMap(friend -> users.findOptionallyById(friend.getId().getOtherUserId()).stream())
                .map(UserView::new)
                .collect(Collectors.toList());
    }

}
