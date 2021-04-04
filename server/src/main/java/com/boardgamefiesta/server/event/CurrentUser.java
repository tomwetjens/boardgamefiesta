package com.boardgamefiesta.server.event;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;

import javax.websocket.Session;
import java.security.Principal;
import java.util.Optional;

class CurrentUser {

    static Optional<User.Id> getUserId(Session session, Users users) {
        return currentUserPrincipalName(session)
                .flatMap(users::findByCognitoUsername);
    }

    private static Optional<String> currentUserPrincipalName(Session session) {
        return Optional.ofNullable(session.getUserPrincipal())
                .map(Principal::getName)
                .filter(name -> !name.isBlank());
    }

}
