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

package com.boardgamefiesta.lambda.http;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.List;

@Provider
public class CorsFilter implements ContainerResponseFilter {

    @ConfigProperty(name = "quarkus.http.cors", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "quarkus.http.cors.origins", defaultValue = "*")
    List<String> origins;

    @ConfigProperty(name = "quarkus.http.cors.headers", defaultValue = "")
    String headers;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (enabled) {
            var origin = requestContext.getHeaderString("Origin");
            if (origins.contains("*") || origins.contains(origin)) {
                responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            }

            responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", headers);
        }
    }
}
