package com.wetjens.gwt;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    @Nested
    class Create {

        @Test
        void beginner() {
            Game game = new Game(new HashSet<>(Arrays.asList(Player.BLUE, Player.RED)), true, new Random(0));

            assertThat(game.getPlayers().get(0)).isEqualTo(Player.WHITE);
            assertThat(game.getPlayers().get(1)).isEqualTo(Player.YELLOW);

            assertThat(game.getCurrentPlayer()).isEqualByComparingTo(Player.WHITE);

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
}
