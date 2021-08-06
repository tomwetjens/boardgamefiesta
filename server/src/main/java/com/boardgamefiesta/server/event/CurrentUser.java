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

package com.boardgamefiesta.server.event;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;

import javax.websocket.Session;
import java.security.Principal;
import java.util.Optional;

class CurrentUser {

    static Optional<User.Id> getUserId(Session session, Users users) {
        return currentUserPrincipalName(session)
                .flatMap(users::findIdByCognitoUsername);
    }

    private static Optional<String> currentUserPrincipalName(Session session) {
        return Optional.ofNullable(session.getUserPrincipal())
                .map(Principal::getName)
                .filter(name -> !name.isBlank());
    }

}
