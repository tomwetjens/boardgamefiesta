package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    private Player playerRed = new Player("Red", PlayerColor.RED);
    private Player playerGreen = new Player("Green", PlayerColor.GREEN);

    @Nested
    class PoliceStation {

        @Test
        void leaveAssistantWithSmuggler() {
            // Given
            var game = new Game(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));

            // When
            game.perform(new Action.LeaveAssistant(), new Random(0));

            // Then
            assertThat(game.getPoliceStation().getMerchants()).containsExactly(game.getPlayerState(playerRed).getMerchant());
            assertThat(game.getPoliceStation().getAssistants()).containsEntry(PlayerColor.RED, 1);
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.SendFamilyMember.class);
        }

        @Test
        void smugglerAfterPerformSendFamilyMember() {
            // Given
            var game = new Game(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.perform(new Action.SendFamilyMember(game.getSpiceWarehouse()), new Random(0));
            game.perform(new Action.MaxSpice(), new Random(0));

            // When
            assertThat(game.getPossibleActions()).containsExactly(Action.Smuggler.class);
            game.perform(new Action.Smuggler(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Fruit.class,
                    Action.Take1Spice.class, Action.Take1Fabric.class, Action.Take1Blue.class);
        }

        @Test
        void smugglerAfterSkipSendFamilyMember() {
            // Given
            var game = new Game(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.skip(new Random(0));

            // When
            assertThat(game.getPossibleActions()).containsExactly(Action.Smuggler.class);
            game.perform(new Action.Smuggler(), new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(Action.Take1Fruit.class,
                    Action.Take1Spice.class, Action.Take1Fabric.class, Action.Take1Blue.class);
        }

        @Test
        void sendFamilyMember() {
            // Given
            var game = new Game(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));

            // When
            game.perform(new Action.SendFamilyMember(game.getSpiceWarehouse()), new Random(0));

            // Then
            assertThat(game.getPoliceStation().getFamilyMembers()).containsExactly(playerGreen);
            assertThat(game.getSpiceWarehouse().getFamilyMembers()).containsExactly(playerRed);
            assertThat(game.getPossibleActions()).containsExactly(Action.MaxSpice.class);
        }

        @Test
        void sendFamilyMemberToPlaceWithGovernor() {
            // Given
            var game = new Game(new LinkedHashSet<>(List.of(playerRed, playerGreen)), LayoutType.SHORT_PATHS, new Random(0));
            game.perform(new Action.Move(game.getPoliceStation()), new Random(0));
            game.perform(new Action.LeaveAssistant(), new Random(0));
            game.perform(new Action.SendFamilyMember(game.getLargeMarket()), new Random(0));

            // When
            game.skip(new Random(0));

            // Then
            assertThat(game.getPossibleActions()).containsExactly(Action.Governor.class);
        }

        @Test
        void sendFamilyMemberToPlaceWithOtherFamilyMember() {
// TODO
        }

    }

    @Test
    void catchFamilyMember() {
        // TODO
    }

    @Test
    void catchFamilyMembers() {
        // TODO
    }

    @Test
    void catchFamilyMembersAtPlaceWithGovernor() {
        // TODO
    }

    @Test
    void catchFamilyMembersAtPlaceWithSmuggler() {
        // TODO
    }

    @Test
    void catchFamilyMembersAtPlaceWithSmugglerAndGovernor() {
        // TODO
    }
}