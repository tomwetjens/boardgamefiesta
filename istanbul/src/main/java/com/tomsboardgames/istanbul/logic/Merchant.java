package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Optional;

@AllArgsConstructor
public class Merchant implements Serializable {

    private static final long serialVersionUID = 1L;

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
}
