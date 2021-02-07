package com.boardgamefiesta.gwt;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.gwt.logic.Automa;
import com.boardgamefiesta.gwt.logic.Game;
import com.boardgamefiesta.gwt.view.ActionType;
import com.boardgamefiesta.gwt.view.StateView;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public class GWT implements GameProvider<Game> {

    public static final String ID = "gwt";

    private static final Duration DEFAULT_TIME_LIMIT = Duration.of(3, ChronoUnit.MINUTES);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return EnumSet.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.GREEN);
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
    public Game start(Set<Player> players, Options options, Random random) {
        return Game.start(players, Game.Options.builder()
                .mode(options.getEnum("mode", Game.Options.Mode.class, Game.Options.Mode.ORIGINAL))
                .buildings(options.getEnum("buildings", Game.Options.Buildings.class, Game.Options.Buildings.RANDOMIZED))
                .playerOrder(options.getEnum("playerOrder", Game.Options.PlayerOrder.class, Game.Options.PlayerOrder.RANDOMIZED))
                .variant(options.getEnum("variant", Game.Options.Variant.class, Game.Options.Variant.ORIGINAL))
                .stationMasterPromos(options.getBoolean("stationMasterPromos", false))
                .building11(options.getBoolean("building11", false))
                .building13(options.getBoolean("building13", false))
                .railsToTheNorth(options.getBoolean("railsToTheNorth", false))
                .build(), random);
    }

    @Override
    public void executeAutoma(Game state, Player player, Random random) {
        new Automa().execute(state, player, random);
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
    public StateSerializer<Game> getStateSerializer() {
        return Game::serialize;
    }

    @Override
    public StateDeserializer<Game> getStateDeserializer() {
        return Game::deserialize;
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
