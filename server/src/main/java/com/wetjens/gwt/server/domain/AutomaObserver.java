package com.wetjens.gwt.server.domain;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
@Slf4j
class AutomaObserver {

    private final AutomaScheduler automaScheduler;

    @Inject
    AutomaObserver(@NonNull AutomaScheduler automaScheduler) {
        this.automaScheduler = automaScheduler;
    }

    public void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Game.StateChanged stateChanged) {
        log.debug("stateChanged: {}", stateChanged);

        var game = stateChanged.getGame();

        if (game.getCurrentPlayer().getType() == Player.Type.COMPUTER) {
            automaScheduler.schedule(game);
        }
    }

}
