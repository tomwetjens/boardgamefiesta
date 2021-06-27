package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class GWTTest {

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

    private static final GWT.Options BEGINNER = GWT.Options.builder()
            .buildings(GWT.Options.Buildings.BEGINNER)
            .build();

    private static final GWT.Options RANDOMIZED = GWT.Options.builder()
            .buildings(GWT.Options.Buildings.RANDOMIZED)
            .build();

    private Player playerA = new Player("Player A", PlayerColor.WHITE, Player.Type.HUMAN);
    private Player playerB = new Player("Player B", PlayerColor.YELLOW, Player.Type.HUMAN);
    private Player playerC = new Player("Player C", PlayerColor.BLUE, Player.Type.HUMAN);

    @Nested
    class Create {

        @Test
        void beginner() {
            GWT game = GWT.start(new LinkedHashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, new Random(0));

            assertThat(game.getStatus()).isEqualTo(GWT.Status.STARTED);
            assertThat(game.getPlayerOrder()).containsExactly(playerA, playerB);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerA);

            // Neutral buildings should not be randomized
            assertThat(Arrays.asList(
                    ((Location.BuildingLocation) game.getTrail().getLocation("A")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("B")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("C")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("D")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("E")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("F")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("G")).getBuilding().get())).extracting(Building::getName)
                    .containsExactly("A", "B", "C", "D", "E", "F", "G");

            // Player buildings should not be randomized
            assertThat(game.currentPlayerState().getBuildings()).extracting(PlayerBuilding::getClass).containsExactlyInAnyOrder(A_BUILDINGS);

            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);
        }

        @Test
        void randomized() {
            GWT game = GWT.start(new LinkedHashSet<>(Arrays.asList(playerA, playerB)), RANDOMIZED, new Random(0));

            assertThat(game.getStatus()).isEqualTo(GWT.Status.STARTED);
            assertThat(game.getPlayerOrder()).containsExactly(playerA, playerB);
            assertThat(game.getCurrentPlayer()).isEqualTo(playerA);
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);

            // Neutral buildings should be randomized
            assertThat(Arrays.asList(
                    ((Location.BuildingLocation) game.getTrail().getLocation("A")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("B")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("C")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("D")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("E")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("F")).getBuilding().get(),
                    ((Location.BuildingLocation) game.getTrail().getLocation("G")).getBuilding().get())).extracting(Building::getName)
                    .doesNotContainSequence("A", "B", "C", "D", "E", "F", "G");

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
            var game = GWT.start(new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, new Random(0));

            // TODO
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
            GWT game = GWT.start(new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, new Random(0));

            assertThat(game.removeDisc(Collections.singletonList(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);
        }

        @Test
        void needBlackOrWhite() {
            GWT game = GWT.start(new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, new Random(0));

            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
        }

        @Test
        void needWhiteButCanOnlyUnlockBlack() {
            GWT game = GWT.start(new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, new Random(0));

            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);

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
            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
            // Normal case to allow black
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
        }

        @Test
        void cannotUnlockButStationsUpgraded() {
            GWT game = GWT.start(new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, new Random(0));
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
            game.getRailroadTrack().upgradeStation(game, game.getRailroadTrack().getStations().get(0));

            // Player is forced to remove disc from station
            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.DowngradeStation.class);
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.DowngradeStation.class);
        }

        @Test
        void cannotUnlockNoStationsUpgraded() {
            GWT game = GWT.start(new HashSet<>(Arrays.asList(playerA, playerB)), BEGINNER, new Random(0));
            // Enough money to pay for unlocks
            game.currentPlayerState().gainDollars(10);

            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);

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
            assertThat(game.removeDisc(Collections.singleton(DiscColor.WHITE)).getActions()).isEmpty();
            assertThat(game.removeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions()).isEmpty();
        }
    }

    @Nested
    class BiddingTest {

        @Test
        void startWhenAllPositionsUncontested() {
            var game = GWT.start(new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(new Action.PlaceBid(new Bid(2, 0)), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(new Action.PlaceBid(new Bid(1, 0)), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(new Action.PlaceBid(new Bid(0, 0)), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);
        }

        @Test
        void skipTurnWhenUncontested() {
            var game = GWT.start(new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC)), GWT.Options.builder()
                    .playerOrder(GWT.Options.PlayerOrder.BIDDING)
                    .build(), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(new Action.PlaceBid(new Bid(0, 0)), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(new Action.PlaceBid(new Bid(1, 0)), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerA);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(new Action.PlaceBid(new Bid(1, 1)), new Random(0));

            assertThat(game.getCurrentPlayer()).describedAs("should skip player C, since bid is uncontested").isSameAs(playerB);
            assertThat(game.possibleActions()).containsExactly(Action.PlaceBid.class);

            game.perform(new Action.PlaceBid(new Bid(2, 0)), new Random(0));

            assertThat(game.getCurrentPlayer()).isSameAs(playerC);
            assertThat(game.possibleActions()).containsExactlyInAnyOrder(Action.Move.class);
        }
    }
}
