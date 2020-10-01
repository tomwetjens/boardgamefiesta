package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.server.domain.Game;
import com.boardgamefiesta.server.domain.Table;
import com.boardgamefiesta.server.domain.User;
import com.boardgamefiesta.server.domain.Users;
import com.boardgamefiesta.server.domain.rating.Ratings;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/users/{userId}/ratings")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class UserRatingResource {

    @Inject
    private Ratings ratings;

    @Inject
    private Users users;

    @GET
    public List<RatingView> getRatings(@PathParam("userId") String userId,
                                       @QueryParam("gameId") String gameId,
                                       @QueryParam("tableId") String tableId) {
        if (tableId != null && !"".equals(tableId)) {
            return ratings.findByTable(User.Id.of(userId), Table.Id.of(tableId))
                    .map(rating -> {
                        var userMap = rating.getDeltas().keySet().stream()
                                .flatMap(deltaUserId -> users.findOptionallyById(deltaUserId).stream())
                                .collect(Collectors.toMap(User::getId, Function.identity()));
                        return new RatingView(rating, userMap);
                    })
                    .map(Collections::singletonList)
                    .orElseThrow(NotFoundException::new);
        } else {
            return ratings.findHistoric(User.Id.of(userId), Game.Id.of(gameId),
                    Instant.now().minus(365, ChronoUnit.DAYS),
                    Instant.now())
                    .map(rating -> new RatingView(rating, Collections.emptyMap()))
                    .collect(Collectors.toList());
        }
    }

}
