package com.wetjens.gwt.server.domain;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
@Slf4j
public class AutomaScheduler {

    // TODO Make thread pool configurable
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    void schedule(Game game) {
        // TODO Send to external queue so it is persisted and can be picked by any worker
        var request = new Request(game);

        // Submit to executor, so CDI event is processed async
        executorService.submit(() -> {
            try {
                CDI.current().getBeanManager().fireEvent(request);
            } catch (RuntimeException e) {
                log.error("Error executing request ", e);
            }
        });
    }

    @Value
    public class Request {
        Game game;
    }

}
