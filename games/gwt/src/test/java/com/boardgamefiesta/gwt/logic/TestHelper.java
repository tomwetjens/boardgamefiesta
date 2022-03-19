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
import com.boardgamefiesta.api.domain.PlayerColor;

import java.util.Random;
import java.util.Set;

public class TestHelper {

    static final Class[] A_BUILDINGS = {
            PlayerBuilding.Building1A.class,
            PlayerBuilding.Building2A.class,
            PlayerBuilding.Building3A.class,
            PlayerBuilding.Building4A.class,
            PlayerBuilding.Building5A.class,
            PlayerBuilding.Building6A.class,
            PlayerBuilding.Building7A.class,
            PlayerBuilding.Building8A.class,
            PlayerBuilding.Building9A.class,
            PlayerBuilding.Building10A.class
    };

    static final GWT.Options BEGINNER = GWT.Options.builder()
            .buildings(GWT.Options.Buildings.BEGINNER)
            .build();

    static final Player PLAYER_A = new Player("Player A", PlayerColor.WHITE, Player.Type.HUMAN);
    static final Player PLAYER_B = new Player("Player B", PlayerColor.YELLOW, Player.Type.HUMAN);
    static final Player PLAYER_C = new Player("Player C", PlayerColor.BLUE, Player.Type.HUMAN);
    static final Player PLAYER_D = new Player("Player D", PlayerColor.RED, Player.Type.HUMAN);

    static GWT givenAGame() {
        return GWT.start(GWT.Edition.FIRST, Set.of(PLAYER_A, PLAYER_B, PLAYER_C, PLAYER_D), BEGINNER, null, new Random(0));
    }

}
