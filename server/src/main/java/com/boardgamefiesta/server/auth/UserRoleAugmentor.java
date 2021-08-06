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

package com.boardgamefiesta.server.auth;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;

@ApplicationScoped
public class UserRoleAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        return Uni.createFrom().item(build(identity));
        // Do 'return context.runBlocking(build(identity));' if a blocking call is required to customize the identity
    }

    private Supplier<SecurityIdentity> build(SecurityIdentity identity) {
        if(identity.isAnonymous() || identity.hasRole(Roles.USER)) {
            return () -> identity;
        } else {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

            // A user with a valid JWT always has role 'user', even if the user has no group in Cognito
            // This eliminates the need for a pre-token generation Lambda which slows things down
            builder.addRole(Roles.USER);

            return builder::build;
        }
    }
}
