package com.wetjens.gwt;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

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

    private Player playerA = new Player("Player A", PlayerColor.WHITE);
    private Player playerB = new Player("Player B", PlayerColor.YELLOW);

    @Nested
    class Create {

        @Test
        void beginner() {
            Game game = new Game(new LinkedHashSet<>(Arrays.asList(playerA, playerB)), true, new Random(0));

            assertThat(game.getPlayers()).containsExactly(playerA, playerB);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerA);

            // Neutral buildings should not be randomized
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("A")).getBuilding().get()).isInstanceOf(NeutralBuilding.A.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("B")).getBuilding().get()).isInstanceOf(NeutralBuilding.B.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("C")).getBuilding().get()).isInstanceOf(NeutralBuilding.C.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("D")).getBuilding().get()).isInstanceOf(NeutralBuilding.D.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("E")).getBuilding().get()).isInstanceOf(NeutralBuilding.E.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("F")).getBuilding().get()).isInstanceOf(NeutralBuilding.F.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("G")).getBuilding().get()).isInstanceOf(NeutralBuilding.G.class);

            // Player buildings should not be randomized
            assertThat(game.currentPlayerState().getBuildings()).extracting(PlayerBuilding::getClass).containsExactlyInAnyOrder(A_BUILDINGS);

            assertThat(game.possibleActions()).containsExactly(Action.Move.class);
        }

        @Test
        void randomized() {
            Game game = new Game(new LinkedHashSet<>(Arrays.asList(playerA, playerB)), false, new Random(0));

            assertThat(game.getPlayers()).containsExactly(playerA, playerB);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerA);
            assertThat(game.possibleActions()).containsExactly(Action.Move.class);

            // Neutral buildings should be randomized
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("A")).getBuilding().get()).isInstanceOf(NeutralBuilding.D.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("B")).getBuilding().get()).isInstanceOf(NeutralBuilding.B.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("C")).getBuilding().get()).isInstanceOf(NeutralBuilding.E.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("D")).getBuilding().get()).isInstanceOf(NeutralBuilding.A.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("E")).getBuilding().get()).isInstanceOf(NeutralBuilding.G.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("F")).getBuilding().get()).isInstanceOf(NeutralBuilding.C.class);
            assertThat(((Location.BuildingLocation) game.getTrail().getLocation("G")).getBuilding().get()).isInstanceOf(NeutralBuilding.F.class);

            // Player buildings should be randomized
            Class[] randomizedBuildings = {
                    PlayerBuilding.Building1A.class,
                    PlayerBuilding.Building2B.class,
                    PlayerBuilding.Building3A.class,
                    PlayerBuilding.Building4A.class,
                    PlayerBuilding.Building5B.class,
                    PlayerBuilding.Building6A.class,
                    PlayerBuilding.Building7B.class,
                    PlayerBuilding.Building8A.class,
                    PlayerBuilding.Building9A.class,
                    PlayerBuilding.Building10B.class
            };
            assertThat(game.currentPlayerState().getBuildings()).extracting(PlayerBuilding::getClass).containsExactlyInAnyOrder(randomizedBuildings);
        }
    }

    @Nested
    class Serialize {

        @Test
        void serialize() {
            State state = new Game(new HashSet<>(Arrays.asList(playerA, playerB)), true, new Random(0));

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            state.serialize(byteArrayOutputStream);

            State deserialized = Game.deserialize(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        }
    }

    @Nested
    class GetPossibleActions {

        @Test
        void noObjectiveCard() {

        }

        @Test
        void hasObjectiveCard() {

        }

        @Test
        void hasObjectiveCardButImmediateActions() {

        }
    }

    @Nested
    class PlaceDisc {

        @Test
        void needWhite() {
            Game game = new Game(new HashSet<>(Arrays.asList(playerA, playerB)), true, new Random(0));

            assertThat(game.placeDisc(Collections.singletonList(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);
        }

        @Test
        void needBlackOrWhite() {
            Game game = new Game(new HashSet<>(Arrays.asList(playerA, playerB)), true, new Random(0));

            assertThat(game.placeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
        }

        @Test
        void needWhiteButCanOnlyUnlockBlack() {
            Game game = new Game(new HashSet<>(Arrays.asList(playerA, playerB)), true, new Random(0));

            assertThat(game.placeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);

            // Remove all white discs
            game.currentPlayerState().unlock(Unlockable.AUX_GAIN_DOLLAR);
            game.currentPlayerState().unlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_4);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);

            // Should allow removal of black by exception
            assertThat(game.placeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
            // Normal case to allow black
            assertThat(game.placeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
        }

        @Test
        void cannotUnlockButStationsUpgraded() {
            Game game = new Game(new HashSet<>(Arrays.asList(playerA, playerB)), true, new Random(0));
            // Enough money to pay for unlocks
            game.currentPlayerState().gainDollars(10);

            // Remove all discs
            game.currentPlayerState().unlock(Unlockable.AUX_GAIN_DOLLAR);
            game.currentPlayerState().unlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_4);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_6);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_DOLLARS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_POINTS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);

            // Place one on a station
            game.getRailroadTrack().getStations().get(0).upgrade(game);

            // Player is forced to remove disc from station
            assertThat(game.placeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.DowngradeStation.class);
            assertThat(game.placeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.DowngradeStation.class);
        }

        @Test
        void cannotUnlockNoStationsUpgraded() {
            Game game = new Game(new HashSet<>(Arrays.asList(playerA, playerB)), true, new Random(0));
            // Enough money to pay for unlocks
            game.currentPlayerState().gainDollars(10);

            assertThat(game.placeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);
            assertThat(game.placeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);

            // Remove all discs
            game.currentPlayerState().unlock(Unlockable.AUX_GAIN_DOLLAR);
            game.currentPlayerState().unlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_4);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD);
            game.currentPlayerState().unlock(Unlockable.CERT_LIMIT_6);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_DOLLARS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_STEP_POINTS);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);
            game.currentPlayerState().unlock(Unlockable.EXTRA_CARD);

            // Player has no discs on stations
            // So player cannot do anything = exception case -> no immediate actions
            assertThat(game.placeDisc(Collections.singleton(DiscColor.WHITE)).getActions()).isEmpty();
            assertThat(game.placeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions()).isEmpty();
        }
    }
}
