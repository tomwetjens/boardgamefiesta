package com.boardgamefiesta.istanbul;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.EventListener;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.istanbul.logic.Action;
import com.boardgamefiesta.istanbul.logic.Automa;
import com.boardgamefiesta.istanbul.logic.Istanbul;
import com.boardgamefiesta.istanbul.logic.LayoutType;
import com.boardgamefiesta.istanbul.view.ActionView;
import com.boardgamefiesta.istanbul.view.IstanbulView;

import javax.json.JsonObject;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

public class IstanbulProvider implements GameProvider<Istanbul> {

    public static final String ID = "big-bazar";

    private static final Duration DEFAULT_TIME_LIMIT = Duration.ofMinutes(10);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return Istanbul.SUPPORTED_COLORS;
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
    public Istanbul start(Set<Player> players, Options options, EventListener eventListener, Random random) {
        var layoutType = options.getEnum("layoutType", LayoutType.class, LayoutType.RANDOM);
        return Istanbul.start(players, layoutType, eventListener, random);
    }

    @Override
    public void executeAutoma(Istanbul state, Player player, Random random) {
        new Automa().execute(state, player, random);
    }

    @Override
    public ViewMapper<Istanbul> getViewMapper() {
        return IstanbulView::new;
    }

    @Override
    public StateSerializer<Istanbul> getStateSerializer() {
        return Istanbul::serialize;
    }

    @Override
    public StateDeserializer<Istanbul> getStateDeserializer() {
        return Istanbul::deserialize;
    }

    @Override
    public boolean hasAutoma() {
        return true;
    }

    @Override
    public Duration getTimeLimit(Options options) {
        return DEFAULT_TIME_LIMIT;
    }

    @Override
    public ActionMapper<Istanbul> getActionMapper() {
        return this::toAction;
    }

    private Action toAction(JsonObject jsonObject, Istanbul state) {
        var type = ActionView.valueOf(jsonObject.getString("type"));
        return type.toAction(jsonObject, state);
    }
}
