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
        return GWT.start(GWT.Edition.SECOND, Set.of(PLAYER_A, PLAYER_B, PLAYER_C, PLAYER_D), BEGINNER, null, new Random(0));
    }

}
