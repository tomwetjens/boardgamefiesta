package com.wetjens.gwt.server.domain;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
@Slf4j
@Transactional
class AutomaExecutor {

    private final Games games;

    @Inject
    AutomaExecutor(@NonNull Games games) {
        this.games = games;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(@Observes AutomaScheduler.Request request) {
        try {
            var game = request.getGame();

            if (game.getStatus() != Game.Status.STARTED) {
                return;
            }

            var currentPlayer = game.getCurrentPlayer();
            if (currentPlayer.getType() != Player.Type.COMPUTER) {
                return;
            }

            game.executeAutoma();

            games.update(game);
            log.debug("saved after automa");
        } catch (RuntimeException e) {
            log.error("Error executing request", e);
            throw e;
        }
    }

}
