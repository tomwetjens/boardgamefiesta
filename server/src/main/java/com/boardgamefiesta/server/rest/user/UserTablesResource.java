package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.domain.featuretoggle.FeatureToggle;
import com.boardgamefiesta.domain.featuretoggle.FeatureToggles;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.table.view.TableView;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/users/{userId}/tables")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
@Slf4j
@Transactional
public class UserTablesResource {

    @Inject
    FeatureToggles featureToggles;

    @Inject
    Tables tables;

    @Inject
    Users users;

    @Inject
    Ratings ratings;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<TableView> getTables(@PathParam("userId") String userIdStr) {
        var userMap = new HashMap<User.Id, User>();
        var ratingMap = new HashMap<User.Id, Rating>();
        var currentUserId = currentUser.getId();
        var userId = User.Id.of(userIdStr);

        return tables.findAll(userId, 10)
                .filter(table -> userId.equals(currentUserId) || FeatureToggle.Id.forGameId(table.getGame().getId())
                        .map(featureToggleId -> featureToggles.findById(featureToggleId)
                                .map(featureToggle -> featureToggle.isEnabled(currentUserId))
                                .orElse(false))
                        .orElse(true))
                .map(table -> new TableView(table, getUserMap(table, userMap), getRatingMap(table, ratingMap), currentUser.getId()))
                .collect(Collectors.toList());
    }

    private Map<User.Id, User> getUserMap(Table table, Map<User.Id, User> userMap) {
        table.getPlayers().forEach(player -> player.getUserId().ifPresent(userId ->
                userMap.computeIfAbsent(userId, k -> users.findById(userId).orElse(null))));
        return userMap;
    }

    private Map<User.Id, Rating> getRatingMap(Table table, Map<User.Id, Rating> ratingMap) {
        table.getPlayers().forEach(player -> player.getUserId().ifPresent(userId ->
                ratingMap.computeIfAbsent(userId, k -> ratings.findLatest(userId, table.getGame().getId(), Instant.now()))));
        return ratingMap;
    }
}
