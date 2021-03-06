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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerBuildingTest {

    @Nested
    class Building8BTest {

        @Test
        void shouldCopyAdjacent() {
            // Given
            var game = TestHelper.givenAGame();
            var player = game.getCurrentPlayer();

            var a = game.getTrail().getBuildingLocation("A").get();
            var a1 = game.getTrail().getBuildingLocation("A-1").get();
            a1.placeBuilding(new PlayerBuilding.Building8B(player));

            // When
            game.perform(new Action.Move(List.of(a1)), new Random(0));
            game.perform(new Action.UseAdjacentBuilding(a), new Random(0));

            // Then
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                    Action.Discard1Guernsey.class,
                    Action.HireWorker.class,
                    Action.HireWorkerPlus2.class,
                    Action.SingleAuxiliaryAction.class);
        }

        @Test
        void shouldAlsoCopyRiskActions() {
            // Given
            var game = TestHelper.givenAGame();
            var player = game.getCurrentPlayer();

            var floodRisk1 = game.getTrail().getBuildingLocation("FLOOD-RISK-1").get();
            var floodRisk2 = game.getTrail().getBuildingLocation("FLOOD-RISK-2").get();
            floodRisk1.placeBuilding(new PlayerBuilding.Building8B(player));
            floodRisk2.placeBuilding(new PlayerBuilding.Building1A(player));

            // When
            game.perform(new Action.Move(List.of(floodRisk1)), new Random(0));
            game.perform(new Action.UseAdjacentBuilding(floodRisk2), new Random(0));

            // Then
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                    // Risk action
                    Action.Discard1JerseyToGain1CertificateAnd2Dollars.class,
                    // Copied adjacent building actions
                    Action.Gain2DollarsPerBuildingInWoods.class,
                    Action.SingleAuxiliaryAction.class);
        }

        @Test
        void shouldAllowOriginalAndCopiedRiskActions() {
            // Given
            var game = TestHelper.givenAGame();
            var player = game.getCurrentPlayer();
            game.playerState(player).addCardToHand(new Card.CattleCard(CattleType.JERSEY, 0, 1));

            var floodRisk1 = game.getTrail().getBuildingLocation("FLOOD-RISK-1").get();
            var floodRisk2 = game.getTrail().getBuildingLocation("FLOOD-RISK-2").get();
            floodRisk1.placeBuilding(new PlayerBuilding.Building8B(player));
            floodRisk2.placeBuilding(new PlayerBuilding.Building1A(player));

            // When
            game.perform(new Action.Move(List.of(floodRisk1)), new Random(0));
            game.perform(new Action.UseAdjacentBuilding(floodRisk2), new Random(0));
            // Perform copied building action
            game.perform(new Action.Gain2DollarsPerBuildingInWoods(), new Random(0));
            // Perform copied risk action
            game.perform(new Action.Discard1JerseyToGain1CertificateAnd2Dollars(), new Random(0));

            // Then
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                    // Original risk action
                    Action.Discard1JerseyToGain1CertificateAnd2Dollars.class);
        }

        @Test
        void shouldCopyAdjacent8B() {
            // Given
            var game = TestHelper.givenAGame();
            var player = game.getCurrentPlayer();
            var otherPlayer = game.getNextPlayer();

            var a2 = game.getTrail().getBuildingLocation("A-2").get();
            var a3 = game.getTrail().getBuildingLocation("A-3").get();
            var b = game.getTrail().getBuildingLocation("B").get();
            a2.placeBuilding(new PlayerBuilding.Building8B(player));
            a3.placeBuilding(new PlayerBuilding.Building8B(otherPlayer));

            // When
            game.perform(new Action.Move(List.of(a2)), new Random(0));
            game.perform(new Action.UseAdjacentBuilding(a3), new Random(0));
            game.perform(new Action.UseAdjacentBuilding(b), new Random(0));

            // Then
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                    Action.Discard1DutchBeltToGain2Dollars.class,
                    Action.PlaceBuilding.class,
                    Action.SingleAuxiliaryAction.class);
        }

        @Test
        void shouldCopyAdjacent8BButNotLoop() {
            // Given
            var game = TestHelper.givenAGame();
            var player = game.getCurrentPlayer();
            var otherPlayer = game.getNextPlayer();

            var a1 = game.getTrail().getBuildingLocation("A-1").get();
            var a2 = game.getTrail().getBuildingLocation("A-2").get();
            a1.placeBuilding(new PlayerBuilding.Building8B(player));
            a2.placeBuilding(new PlayerBuilding.Building8B(otherPlayer));

            // When
            game.perform(new Action.Move(List.of(a1)), new Random(0));
            game.perform(new Action.UseAdjacentBuilding(a2), new Random(0));

            // Then
            assertThatThrownBy(() -> game.perform(new Action.UseAdjacentBuilding(a1), new Random(0)))
                    .isInstanceOf(GWTException.class)
                    .hasMessage(GWTError.CANNOT_PERFORM_ACTION.name());
        }

        @Test
        void should10BThen8B() {
            // Given
            var game = TestHelper.givenAGame();
            var player = game.getCurrentPlayer();

            var a1 = game.getTrail().getBuildingLocation("A-1").get();
            var a2 = game.getTrail().getBuildingLocation("A-2").get();
            a1.placeBuilding(new PlayerBuilding.Building10B(player));
            a2.placeBuilding(new PlayerBuilding.Building8B(player));

            // When
            game.perform(new Action.Move(List.of(a1)), new Random(0));
            game.perform(new Action.Move4Forward(List.of(a2)), new Random(0));
            game.perform(new Action.UseAdjacentBuilding(a1), new Random(0));

            // Then
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                    Action.Gain4Dollars.class,
                    Action.MoveEngineAtMost4Forward.class,
                    Action.Move4Forward.class,
                    Action.SingleAuxiliaryAction.class);
        }

    }

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

            var space = game.getRailroadTrack().getSpace("18");
            game.getRailroadTrack().moveEngineForward(player, space, 0, Integer.MAX_VALUE);

            // When
            game.perform(new Action.Move(List.of(a1)), new Random(0));
            var station = game.getRailroadTrack().getStations().get(0);
            game.perform(new Action.UpgradeAnyStationBehindEngine(station), new Random(0));
            game.perform(new Action.UnlockWhite(Unlockable.AUX_GAIN_DOLLAR), new Random(0));
            game.perform(new Action.AppointStationMaster(Worker.ENGINEER), new Random(0));

            // Then
            assertThat(game.playerState(player).getBalance()).isEqualTo(startBalance - 2);
            assertThat(game.playerState(player).getNumberOfEngineers()).isEqualTo(1);
            assertThat(game.getRailroadTrack().currentSpace(player)).isSameAs(space);
            assertThat(game.getRailroadTrack().getUpgradedBy(station)).contains(player);
            assertThat(game.getRailroadTrack().getStationMaster(station)).isEmpty();
            assertThat(game.getRailroadTrack().getWorker(station)).contains(Worker.ENGINEER);
        }
    }

}