package com.wetjens.gwt;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    @Nested
    class Create {

        @Test
        void beginner() {
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

            assertThat(game.getPlayers().get(0)).isEqualTo(Player.RED);
            assertThat(game.getPlayers().get(1)).isEqualTo(Player.BLUE);

            assertThat(game.getCurrentPlayer()).isEqualByComparingTo(Player.RED);

            assertThat(game.possibleActions()).containsExactly(Action.Move.class);
        }
    }

    @Nested
    class Serialize {

        @Test
        void serialize() throws Exception {
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            game.serialize(byteArrayOutputStream);

            Game deserialized = Game.deserialize(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
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
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

            assertThat(game.placeDisc(Collections.singletonList(DiscColor.WHITE)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockWhite.class);
        }

        @Test
        void needBlackOrWhite() {
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

            assertThat(game.placeDisc(Arrays.asList(DiscColor.WHITE, DiscColor.BLACK)).getActions().get(0).getPossibleActions()).containsExactly(Action.UnlockBlackOrWhite.class);
        }

        @Test
        void needWhiteButCanOnlyUnlockBlack() {
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

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
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

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
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

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
