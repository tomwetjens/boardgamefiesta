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
