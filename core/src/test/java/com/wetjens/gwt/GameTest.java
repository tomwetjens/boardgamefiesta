package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class GameTest {

    @Test
    void test() {
        Game game = new Game(Arrays.asList("A", "B"), new Random(0));

        Player playerA = game.getPlayers().get(0);
        assertThat(playerA.getName()).isEqualTo("A");
        assertThat(playerA.getColor()).isEqualTo(Player.Color.WHITE);

        Player playerB = game.getPlayers().get(1);
        assertThat(playerB.getName()).isEqualTo("B");
        assertThat(playerB.getColor()).isEqualTo(Player.Color.YELLOW);

        assertThat(game.getCurrentPlayer()).isSameAs(playerA);

        assertThat(game.possibleActions()).containsExactly(Action.Move.class);

        game.perform(new Action.Move(Collections.singletonList(game.getTrail().getBuildingLocation(NeutralBuilding.A.class))));

        assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                Action.Discard1Guernsey.class,
                Action.HireWorker.class,
                Action.HireSecondWorker.class,
                Action.SingleAuxiliaryAction.class);

        game.perform(new Action.Discard1Guernsey());

        assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                Action.PlayObjectiveCard.class,
                Action.HireWorker.class,
                Action.HireSecondWorker.class);
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
