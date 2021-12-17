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

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.exception.APIError;
import com.boardgamefiesta.server.rest.exception.APIException;
import com.boardgamefiesta.server.rest.user.view.UserView;

import javax.annotation.security.RolesAllowed;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
public class UsersResource {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_EMAIL_LENGTH = 6;
    private static final int MAX_SEARCH_RESULTS = 5;

    @Inject
    Users users;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<UserView> searchUsers(@QueryParam("q") String q) {
        if (q != null && q.contains("@") && q.length() >= MIN_EMAIL_LENGTH) {
            return users.findByEmail(q)
                    .map(user -> new UserView(user.getId(), user, currentUser.getId()))
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (q != null && q.length() >= MIN_USERNAME_LENGTH) {
            return users.findByUsernameStartsWith(q, MAX_SEARCH_RESULTS)
                    .limit(MAX_SEARCH_RESULTS)
                    .map(user -> new UserView(user.getId(), user, currentUser.getId()))
                    .collect(Collectors.toList());
        } else {
            throw APIException.badRequest(APIError.MUST_SPECIFY_USERNAME_OR_EMAIL);
        }
    }

    @GET
    @Path("/{idOrUsername}")
    public UserView get(@PathParam("idOrUsername") String idOrUsername) {
        User user;
        if (User.Id.check(idOrUsername)) {
            user = users.findById(User.Id.of(idOrUsername))
                    .orElseThrow(NotFoundException::new);
        } else {
            user = users.findByUsername(idOrUsername)
                    .orElseThrow(NotFoundException::new);
        }
        return new UserView(user.getId(), user, currentUser.getId());
    }

    @POST
    @Path("/{id}/change-location")
    public void changeLocation(@PathParam("id") String id, ChangeLocationRequest request) {
        var userId = User.Id.of(id);

        checkCurrentUser(userId);

        handleConcurrentModification(userId, user ->
                user.changeLocation(request.getLocation()));
    }

    @POST
    @Path("/{id}/change-language")
    public void changeLanguage(@PathParam("id") String id, ChangeLanguageRequest request) {
        var userId = User.Id.of(id);

        checkCurrentUser(userId);

        handleConcurrentModification(userId, user ->
                user.changeLanguage(request.getLanguage()));
    }

    @POST
    @Path("/{id}/change-time-zone")
    public void changeLanguage(@PathParam("id") String id, ChangeTimeZoneRequest request) {
        var userId = User.Id.of(id);

        checkCurrentUser(userId);

        try {
            var zoneId = ZoneId.of(request.getTimeZone());

            handleConcurrentModification(userId, user ->
                    user.changeTimeZone(zoneId));
        } catch (DateTimeException e) {
            throw APIException.badRequest(APIError.INVALID_TIME_ZONE);
        }
    }

    @POST
    @Path("/{id}/change-email")
    public void changeEmail(@PathParam("id") String id, @Valid ChangeEmailRequest request) {
        var userId = User.Id.of(id);

        checkCurrentUser(userId);

        handleConcurrentModification(userId, user ->
                user.changeEmail(request.getEmail()));
    }

    @POST
    @Path("/{id}/change-password")
    public void changePassword(@PathParam("id") String id, @Valid ChangePasswordRequest request) {
        var userId = User.Id.of(id);

        checkCurrentUser(userId);

        handleConcurrentModification(userId, user ->
                user.changePassword(request.getPassword()));
    }

    private void checkCurrentUser(User.Id userId) {
        if (!userId.equals(currentUser.getId())) {
            throw APIException.forbidden(APIError.NOT_SUPPORTED);
        }
    }

    private User handleConcurrentModification(User.Id id, Consumer<User> modifier) {
        int retries = 0;
        do {
            var table = users.findById(id).orElseThrow(NotFoundException::new);

            modifier.accept(table);

            try {
                users.update(table);

                return table;
            } catch (Users.ConcurrentModificationException e) {
                if (retries >= 1) {
                    throw e;
                }
                retries++;
            }
        } while (true);
    }

}
