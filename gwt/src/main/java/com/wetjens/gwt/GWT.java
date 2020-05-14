package com.wetjens.gwt;

import com.wetjens.gwt.api.Action;
import com.wetjens.gwt.api.Implementation;
import com.wetjens.gwt.api.Player;
import com.wetjens.gwt.api.PlayerColor;
import com.wetjens.gwt.api.State;
import com.wetjens.gwt.view.ActionType;

import javax.json.JsonObject;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GWT implements Implementation {

    @Override
    public String getName() {
        return "GWT";
    }

    @Override
    public Set<PlayerColor> getAvailableColors() {
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
    public State start(Set<Player> players, Map<String, String> options, Random random) {
        return new Game(players, options.getOrDefault("beginner", "false").equals("true"), random);
    }

    @Override
    public void executeAutoma(State state, Random random) {
        new Automa().execute((Game) state, random);
    }

    @Override
    public Action toAction(JsonObject jsonObject, State state) {
        ActionType type = ActionType.valueOf(jsonObject.getString("type"));

        return type.toAction(jsonObject, (Game) state);
    }

    @Override
    public Object toView(State state, Player viewer) {
        return null;
    }

    @Override
    public String toView(Class<? extends Action> action) {
        return ActionType.of((Class<? extends com.wetjens.gwt.Action>) action).name();
    }

    @Override
    public State deserialize(InputStream inputStream) {
        return Game.deserialize(inputStream);
    }
}
