package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerBuildingTest {

    private static final Class[] A_BUILDINGS = {
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

    private static final Game.Options BEGINNER = Game.Options.builder()
            .buildings(Game.Options.Buildings.BEGINNER)
            .build();

    private static final Player PLAYER_A = new Player("Player A", PlayerColor.WHITE);
    private static final Player PLAYER_B = new Player("Player B", PlayerColor.YELLOW);
    private static final Player PLAYER_C = new Player("Player C", PlayerColor.BLUE);
    private static final Player PLAYER_D = new Player("Player D", PlayerColor.RED);

    @Nested
    class Building9BTest {

        @Test
        void shouldAppointStationMaster() {
            // Given
            var game = givenAGame();
            var player = game.getCurrentPlayer();
            var startBalance = game.playerState(player).getBalance();
            game.playerState(player).gainWorker(Worker.ENGINEER, game);

            var a1 = game.getTrail().getBuildingLocation("A-1").get();
            a1.placeBuilding(new PlayerBuilding.Building9B(player));

            var space = game.getRailroadTrack().getSpace(18);
            game.getRailroadTrack().moveEngineForward(player, space, 0, Integer.MAX_VALUE);

            // When
            game.perform(new Action.Move(List.of(a1)), new Random(0));
            game.perform(new Action.UpgradeAnyStationBehindEngine(game.getRailroadTrack().getStations().get(0)), new Random(0));
            game.perform(new Action.UnlockWhite(Unlockable.AUX_GAIN_DOLLAR), new Random(0));
            game.perform(new Action.AppointStationMaster(Worker.ENGINEER), new Random(0));

            // Then
            assertThat(game.playerState(player).getBalance()).isEqualTo(startBalance - 2);
            assertThat(game.playerState(player).getNumberOfEngineers()).isEqualTo(1);
            assertThat(game.getRailroadTrack().currentSpace(player)).isSameAs(space);
            assertThat(game.getRailroadTrack().getStations().get(0).getPlayers()).contains(player);
            assertThat(game.getRailroadTrack().getStations().get(0).getStationMaster()).isEmpty();
            assertThat(game.getRailroadTrack().getStations().get(0).getWorker()).contains(Worker.ENGINEER);
        }
    }

    private Game givenAGame() {
        return Game.start(Set.of(PLAYER_A, PLAYER_B, PLAYER_C, PLAYER_D), BEGINNER, new Random(0));
    }

}