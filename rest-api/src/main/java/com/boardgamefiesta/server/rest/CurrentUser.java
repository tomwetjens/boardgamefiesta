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

package com.boardgamefiesta.server.rest;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import java.util.Optional;

@RequestScoped
public class CurrentUser {

    @Inject
    Users users;

    @Inject
    JsonWebToken jsonWebToken;

    private User.Id id;
    private User user;

    public User.Id getId() {
        return getOptionalId().orElseThrow(() -> new NotAuthorizedException("User not authenticated"));
    }

    public Optional<User.Id> getOptionalId() {
        if (id == null) {
            getOptionalCognitoUsername()
                    .ifPresent(cognitoUsername -> users.findIdByCognitoUsername(cognitoUsername)
                            .ifPresentOrElse(id -> this.id = id, this::createAutomatically));
        }
        return Optional.ofNullable(id);
    }

    private void createAutomatically() {
        var user = User.createAutomatically(getCognitoUsername(), getEmail());

        // TODO Fix race condition where multiple items are created for the same username (in case of parallel requests)
        users.add(user);

        this.user = user;
        this.id = user.getId();
    }

    public User get() {
        if (user == null) {
            var id = getId();
            if (user == null) {
                user = users.findById(id)
                        .orElseThrow(() -> new NotAuthorizedException("User not found"));
            }
        }
        return user;
    }

    private String getCognitoUsername() {
        return getOptionalCognitoUsername().orElseThrow(() -> new NotAuthorizedException("Invalid token"));
    }

    private Optional<String> getOptionalCognitoUsername() {
        return Optional.ofNullable(jsonWebToken.getClaim("cognito:username"));
    }

    private String getEmail() {
        return jsonWebToken.getClaim("email");
    }

}
