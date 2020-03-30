package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import lombok.Getter;

@Getter
public class ObjectiveCard extends Card {

    private final PossibleAction possibleAction;
    private final List<Task> tasks;
    private final int points;
    private final int penalty;

    public ObjectiveCard(PossibleAction possibleAction, List<Task> tasks, int points, int penalty) {
        this.possibleAction = possibleAction;
        this.tasks = tasks;
        this.points = points;
        this.penalty = penalty;
    }

    public static Queue<ObjectiveCard> randomDeck(Random random) {
        List<ObjectiveCard> deck = new ArrayList<>(all());
        Collections.shuffle(deck, random);
        return new LinkedList<>(deck);
    }

    private static Collection<ObjectiveCard> all() {
        return Arrays.asList(
                new ObjectiveCard(PossibleAction.any(SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.any(SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.any(MoveEngine2Or3SpacesForward.class), Arrays.asList(Task.BUILDING, Task.BLUE_TEEPEE, Task.HAZARD, Task.HAZARD), 5, 3),
                new ObjectiveCard(PossibleAction.choice(DrawCardThenDiscardCard.Draw1CardThenDiscard1Card.class, DrawCardThenDiscardCard.Draw2CardsThenDiscard2Cards.class, DrawCardThenDiscardCard.Draw3CardsThenDiscard3Cards.class), Arrays.asList(Task.BUILDING, Task.BUILDING, Task.HAZARD), 5, 2)
                // TODO Add all objective cards
        );
    }

    public enum Task {
        BUILDING,
        GREEN_TEEPEE,
        BLUE_TEEPEE,
        HAZARD,
        TRAIN_STATION,
        BREEDING_VALUE_3,
        WEST_HIGHLAND,
        TEXAS_LONGHORN,
        SAN_FRANCISCO
    }

    public static final class MoveEngine2Or3SpacesForward extends Action {
        private final RailroadTrack.Space to;

        public MoveEngine2Or3SpacesForward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 2, 3);
        }
    }
}