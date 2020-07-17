package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Building {

    @Getter
    private final String name;

    @Getter
    private final Hand hand;

    static Building deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        var name = jsonObject.getString("name");
        if (name.length() == 1) {
            return NeutralBuilding.forName(name);
        } else {
            return PlayerBuilding.forName(name, playerMap.get(jsonObject.getString("player")));
        }
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var builder = factory.createObjectBuilder()
                .add("name", this.name);
        if (this instanceof PlayerBuilding) {
            builder.add("player", ((PlayerBuilding) this).getPlayer().getName());
        }
        return builder.build();
    }

    abstract PossibleAction activate(Game game);

}
