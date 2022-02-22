/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.server.cognito;

import com.boardgamefiesta.domain.user.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CognitoUserUpdaterTest {

    @Mock
    JsonWebToken jsonWebToken;

    CognitoUserUpdater cognitoUserUpdater;

    @BeforeEach
    void setUp() {
        cognitoUserUpdater = new CognitoUserUpdater(CognitoIdentityProviderClient.create(), jsonWebToken);

        lenient().when(jsonWebToken.getIssuer()).thenReturn("https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_4FuZdxo27");
    }

    @Disabled
    @Test
    void manuallyChangeUsername() {
        cognitoUserUpdater.changeUsername(new User.UsernameChanged("akwilliamson", "doubleawilly"));
    }
}