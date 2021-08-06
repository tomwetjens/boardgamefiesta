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

package com.boardgamefiesta.server;

import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.spi.UnhandledException;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

@WebFilter("*")
@Slf4j
public class ExceptionHandlingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (UnhandledException e) {
            var rootCause = getRootCause(e);
            if (rootCause instanceof IllegalStateException
                    && (rootCause.getMessage().startsWith("UT000127: Response has already been sent")
                    || rootCause.getMessage().startsWith("UT000068: Servlet path match failed")
                    || rootCause.getMessage().startsWith("UT010019: Response already commited"))) {
                // Ignore
                log.debug("Ignoring unhandled exception", e);
            } else {
                throw e;
            }
        }
    }

    private static Throwable getRootCause(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
