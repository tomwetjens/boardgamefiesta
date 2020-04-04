package com.wetjens.gwt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class GameTest {

    @Nested
    class Create {

        @Test
        void beginner() {
            Game game = new Game(Arrays.asList("A", "B"), Game.Options.builder().beginner(true).build(), new Random(0));

            Player playerA = game.getPlayers().get(0);
            assertThat(playerA.getName()).isEqualTo("A");
            assertThat(playerA.getColor()).isEqualTo(Player.Color.WHITE);

            Player playerB = game.getPlayers().get(1);
            assertThat(playerB.getName()).isEqualTo("B");
            assertThat(playerB.getColor()).isEqualTo(Player.Color.YELLOW);

            assertThat(game.getCurrentPlayer()).isSameAs(playerA);

            assertThat(game.possibleActions()).containsExactly(Action.Move.class);
        }
    }

    @Nested
    class Serialize {

        @Test
        void serialize() throws Exception {
            Game game = new Game(Arrays.asList("A", "B"), Game.Options.builder().beginner(true).build(), new Random(0));

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
