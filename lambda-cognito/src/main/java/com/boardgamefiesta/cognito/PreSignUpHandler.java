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

package com.boardgamefiesta.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;

@Named("preSignUp")
@Slf4j
public class PreSignUpHandler implements RequestHandler<PreSignUpEvent, PreSignUpEvent> {

    private final Users users;

    @Inject
    public PreSignUpHandler(@NonNull Users users) {
        this.users = users;
    }

    @Override
    public PreSignUpEvent handleRequest(@NonNull PreSignUpEvent event, Context context) {
        try {
            log.info("Pre Sign-up trigger: {}", event);
            var preferredUsername = event.getRequest().getUserAttributes().get("preferred_username");
            var username = preferredUsername != null ? preferredUsername : event.getUserName();
            var email = event.getRequest().getUserAttributes().get("email");

            if (!"PreSignUp_AdminCreateUser".equals(event.getTriggerSource())) {
                User.validateUsername(username);

                users.validateBeforeAdd(email);
            }

            var response = new PreSignUpResponse();
            log.info("Returning from Pre Sign-up trigger: {}", response);

            event.setResponse(response);

            return event;
        } catch (Users.EmailAlreadyInUse | User.UsernameTooShort | User.UsernameTooLong | User.UsernameForbidden | User.UsernameInvalidChars e) {
            // Do not hide useful exceptions
            throw e;
        } catch (RuntimeException e) {
            // Any other error, hide details from client
            log.error("Error occurred in Pre Sign-up trigger: {}", e.getMessage(), e);
            throw new RuntimeException("Unknown error");
        }
    }
}
