package com.boardgamefiesta.istanbul;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.istanbul.logic.Action;
import com.boardgamefiesta.istanbul.logic.Game;
import com.boardgamefiesta.istanbul.logic.LayoutType;
import com.boardgamefiesta.istanbul.view.ActionView;
import com.boardgamefiesta.istanbul.view.IstanbulView;

import javax.json.JsonObject;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

public class Istanbul implements GameProvider<Game> {

    public static final String ID = "istanbul";

    private static final Duration DEFAULT_TIME_LIMIT = Duration.ofSeconds(90);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return Game.SUPPORTED_COLORS;
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 5;
    }

    @Override
    public Game start(Set<Player> players, Options options, Random random) {
        var layoutType = options.getEnum("layoutType", LayoutType.class, LayoutType.RANDOM);
        return Game.start(players, layoutType, random);
    }

    @Override
    public void executeAutoma(Game state, Player player, Random random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ViewMapper<Game> getViewMapper() {
        return IstanbulView::new;
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
        return false;
    }

    @Override
    public Duration getTimeLimit(Options options) {
        return DEFAULT_TIME_LIMIT;
    }

    @Override
    public ActionMapper<Game> getActionMapper() {
        return this::toAction;
    }

    private Action toAction(JsonObject jsonObject, Game state) {
        var type = ActionView.valueOf(jsonObject.getString("type"));
        return type.toAction(jsonObject, state);
    }
}
