package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.Player;
import com.boardgamefiesta.api.PlayerColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class Merchant {

    @Getter
    private final PlayerColor color;

    private final Player player;

    @Getter
    private int assistants;

    static Merchant dummy(PlayerColor color) {
        return new Merchant(color, null, 0);
    }

    static Merchant forPlayer(Player player) {
        return new Merchant(player.getColor(), player, 4);
    }

    void returnAssistants(int amount) {
        this.assistants += amount;
    }

    void removeAssistant() {
        if (this.assistants == 0) {
            throw new IstanbulException(IstanbulError.NO_ASSISTANTS_AVAILABLE);
        }
        this.assistants--;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    static Merchant deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        return new Merchant(
                PlayerColor.valueOf(jsonObject.getString("color")),
                playerMap.get(jsonObject.getString("player")),
                jsonObject.getInt("assistants"));
    }

    JsonObject serialize(JsonBuilderFactory jsonBuilderFactory) {
        return jsonBuilderFactory.createObjectBuilder()
                .add("color", color.name())
                .add("player", player != null ? player.getName() : null)
                .add("assistants", assistants)
                .build();
    }
}
