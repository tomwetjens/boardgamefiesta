package com.boardgamefiesta.server.automa;

import com.boardgamefiesta.server.domain.DomainService;
import com.boardgamefiesta.server.domain.table.Player;
import com.boardgamefiesta.server.domain.table.Table;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Slf4j
class AutomaScheduler implements DomainService {

    // TODO Make thread pool configurable
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    void schedule(Table table, Player player) {
        // TODO Send to external queue so it is persisted and can be picked by any worker

        // Submit to executor, so CDI event is processed async
        executorService.schedule(() -> {
            try {
                CDI.current().getBeanManager().fireEvent(new Request(table, player));
            } catch (RuntimeException e) {
                log.error("Error executing request ", e);
            }
        }, 2, TimeUnit.SECONDS); // simulate computer thinking
    }

    @Value
    static class Request {
        Table table;
        Player player;
    }

}
