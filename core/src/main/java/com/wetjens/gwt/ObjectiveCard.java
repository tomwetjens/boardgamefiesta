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
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngine2Or3Forward.class), Arrays.asList(Task.BUILDING, Task.BLUE_TEEPEE, Task.HAZARD, Task.HAZARD), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.DrawCardsThenDiscardCards.upTo(3)), Arrays.asList(Task.BUILDING, Task.BUILDING, Task.HAZARD), 5, 2)
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
}
