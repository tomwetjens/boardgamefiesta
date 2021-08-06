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

    static Building deserialize(GWT.Edition edition, Map<String, Player> playerMap, JsonObject jsonObject) {
        var name = jsonObject.getString("name");
        if (name.length() == 1) {
            return NeutralBuilding.forName(name);
        } else {
            return PlayerBuilding.forName(edition, name, playerMap.get(jsonObject.getString("player")));
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

    abstract PossibleAction getPossibleAction(GWT game);

}
