/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
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
