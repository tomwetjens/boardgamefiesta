package com.boardgamefiesta.istanbul;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.istanbul.logic.Game;
import com.boardgamefiesta.istanbul.logic.LayoutType;
import com.boardgamefiesta.istanbul.view.ActionView;
import com.boardgamefiesta.istanbul.view.IstanbulView;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class Istanbul implements com.boardgamefiesta.api.domain.Game<Game> {

    public static final Id ID = Id.of("istanbul");

    private static final Duration DEFAULT_TIME_LIMIT = Duration.ofSeconds(90);

    @Override
    public Id getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return com.boardgamefiesta.istanbul.logic.Game.SUPPORTED_COLORS;
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
    public com.boardgamefiesta.istanbul.logic.Game start(Set<Player> players, Options options, Random random) {
        var layoutType = options.getEnum("layoutType", LayoutType.class, LayoutType.RANDOM);
        return com.boardgamefiesta.istanbul.logic.Game.start(players, layoutType, random);
    }

    @Override
    public void executeAutoma(com.boardgamefiesta.istanbul.logic.Game state, Random random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ViewMapper<Game> getViewMapper() {
        return IstanbulView::new;
    }

    @Override
    public StateSerializer<com.boardgamefiesta.istanbul.logic.Game> getStateSerializer() {
        return com.boardgamefiesta.istanbul.logic.Game::serialize;
    }

    @Override
    public StateDeserializer<com.boardgamefiesta.istanbul.logic.Game> getStateDeserializer() {
        return com.boardgamefiesta.istanbul.logic.Game::deserialize;
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

    private com.boardgamefiesta.istanbul.logic.Action toAction(JsonObject jsonObject, com.boardgamefiesta.istanbul.logic.Game state) {
        var type = ActionView.valueOf(jsonObject.getString("type"));
        return type.toAction(jsonObject, state);
    }
}
