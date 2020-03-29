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

        assertThat(game.getPossibleActions()).containsExactly(Move.class);

        game.perform(new Move(Collections.singletonList(game.getTrail().getBuildingLocation(NeutralBuilding.A.class))));

        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(
                NeutralBuilding.A.DiscardOneGuernsey.class,
                NeutralBuilding.A.HireWorker.class,
                NeutralBuilding.A.HireSecondWorker.class,
                Location.BuildingLocation.SingleAuxiliaryAction.class);

        game.perform(new NeutralBuilding.A.DiscardOneGuernsey());

        assertThat(game.getPossibleActions()).containsExactlyInAnyOrder(
                PlayObjectiveCard.class,
                NeutralBuilding.A.HireWorker.class,
                NeutralBuilding.A.HireSecondWorker.class);
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
