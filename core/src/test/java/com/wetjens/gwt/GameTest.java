package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class GameTest {

    @Test
    void test() {
        Game game = new Game(Arrays.asList(Player.BLUE, Player.RED, Player.YELLOW), new Random(0));

        assertThat(game.getCurrentPlayer()).isEqualTo(Player.YELLOW);

        assertThat(game.possibleActions()).containsExactly(Move.class);

        game.perform(new Move(Collections.singletonList(game.getTrail().getBuildingLocation(NeutralBuilding.A.class))));

        assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                Action.DiscardOneGuernsey.class,
                Action.HireWorker.class,
                Action.HireSecondWorker.class,
                Action.SingleAuxiliaryAction.class);

        game.perform(new Action.DiscardOneGuernsey());

        assertThat(game.possibleActions()).containsExactlyInAnyOrder(
                PlayObjectiveCard.class,
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
