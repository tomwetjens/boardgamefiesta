package com.boardgamefiesta.gwt;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.gwt.logic.Automa;
import com.boardgamefiesta.gwt.logic.Game;
import com.boardgamefiesta.gwt.view.ActionType;
import com.boardgamefiesta.gwt.view.StateView;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class GWT implements com.boardgamefiesta.api.domain.Game<Game> {

    public static final Id ID = Id.of("gwt");

    private static final Duration DEFAULT_TIME_LIMIT = Duration.of(3, ChronoUnit.MINUTES);

    @Override
    public com.boardgamefiesta.api.domain.Game.Id getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return EnumSet.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.WHITE);
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 4;
    }

    @Override
    public com.boardgamefiesta.gwt.logic.Game start(Set<Player> players, Options options, Random random) {
        return com.boardgamefiesta.gwt.logic.Game.start(players, options.getBoolean("beginner", false), random);
    }

    @Override
    public void executeAutoma(com.boardgamefiesta.gwt.logic.Game state, Random random) {
        new Automa().execute(state, random);
    }

    @Override
    public ActionMapper<Game> getActionMapper() {
        return ActionType::toAction;
    }

    @Override
    public ViewMapper<Game> getViewMapper() {
        return StateView::new;
    }

    @Override
    public StateSerializer<com.boardgamefiesta.gwt.logic.Game> getStateSerializer() {
        return com.boardgamefiesta.gwt.logic.Game::serialize;
    }

    @Override
    public StateDeserializer<com.boardgamefiesta.gwt.logic.Game> getStateDeserializer() {
        return com.boardgamefiesta.gwt.logic.Game::deserialize;
    }

    @Override
    public boolean hasAutoma() {
        return true;
    }

    @Override
    public Duration getTimeLimit(Options options) {
        return DEFAULT_TIME_LIMIT;
    }
}
