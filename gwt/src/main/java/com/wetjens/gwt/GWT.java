package com.wetjens.gwt;

import com.wetjens.gwt.api.Action;
import com.wetjens.gwt.api.Game;
import com.wetjens.gwt.api.Options;
import com.wetjens.gwt.api.Player;
import com.wetjens.gwt.api.PlayerColor;
import com.wetjens.gwt.api.State;
import com.wetjens.gwt.view.ActionType;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class GWT implements Game {

    @Override
    public String getId() {
        return "gwt";
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
    public State start(Set<Player> players, Options options, Random random) {
        return new com.wetjens.gwt.Game(players, options.getBoolean("beginner", false), random);
    }

    @Override
    public void executeAutoma(State state, Random random) {
        new Automa().execute((com.wetjens.gwt.Game) state, random);
    }

    @Override
    public Action toAction(JsonObject jsonObject, State state) {
        ActionType type = ActionType.valueOf(jsonObject.getString("type"));

        return type.toAction(jsonObject, (com.wetjens.gwt.Game) state);
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
        return com.wetjens.gwt.Game.deserialize(inputStream);
    }
}
