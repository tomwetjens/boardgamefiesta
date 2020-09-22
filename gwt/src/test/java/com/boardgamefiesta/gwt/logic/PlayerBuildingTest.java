package com.boardgamefiesta.gwt.logic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerBuildingTest {

    @Nested
    class Building9BTest {

        @Test
        void shouldAppointStationMaster() {
            // Given
            var game = TestHelper.givenAGame();
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

}