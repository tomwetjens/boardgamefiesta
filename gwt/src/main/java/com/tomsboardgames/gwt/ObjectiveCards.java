package com.tomsboardgames.gwt;

import lombok.NonNull;

import java.io.Serializable;
import java.util.*;

public class ObjectiveCards implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Queue<ObjectiveCard> drawStack;
    private final Set<ObjectiveCard> available;

    ObjectiveCards(Random random) {
        this.drawStack = createDrawStack(random);
        this.available = new HashSet<>();
        fill();
    }

    static Queue<ObjectiveCard> createStartingObjectiveCardsDrawStack(@NonNull Random random) {
        List<ObjectiveCard> deck = new ArrayList<>(createStartingObjectiveCardsSet());
        Collections.shuffle(deck, random);
        return new LinkedList<>(deck);
    }

    private static Collection<ObjectiveCard> createStartingObjectiveCardsSet() {
        return Arrays.asList(
                new ObjectiveCard(null, Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BUILDING), 3, 0),
                new ObjectiveCard(null, Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE), 3, 0),
                new ObjectiveCard(null, Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.HAZARD), 3, 0),
                new ObjectiveCard(null, Arrays.asList(ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 3, 0)
        );
    }

    void remove(ObjectiveCard objectiveCard) {
        if (!available.remove(objectiveCard)) {
            throw new GWTException(GWTError.OBJECTIVE_CARD_NOT_AVAILABLE);
        }

        fill();
    }

    public Set<ObjectiveCard> getAvailable() {
        return Collections.unmodifiableSet(available);
    }

    public int getDrawStackSize() {
        return drawStack.size();
    }

    private void fill() {
        while (available.size() < 4 && !drawStack.isEmpty()) {
            available.add(drawStack.poll());
        }
    }

    private Queue<ObjectiveCard> createDrawStack(Random random) {
        List<ObjectiveCard> deck = new ArrayList<>(createSet());
        Collections.shuffle(deck, random);
        return new LinkedList<>(deck);
    }

    private static Collection<ObjectiveCard> createSet() {
        return Arrays.asList(
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BUILDING), 4, 2),

                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(ObjectiveCard.Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(ObjectiveCard.Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(ObjectiveCard.Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(ObjectiveCard.Task.SAN_FRANCISCO), 5, 3),

                new ObjectiveCard(PossibleAction.whenThen(0, 3, Action.DrawCard.class, Action.DiscardCard.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.whenThen(0, 3, Action.DrawCard.class, Action.DiscardCard.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.whenThen(0, 3, Action.DrawCard.class, Action.DiscardCard.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_5, ObjectiveCard.Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.whenThen(0, 3, Action.DrawCard.class, Action.DiscardCard.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.GREEN_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.whenThen(0, 3, Action.DrawCard.class, Action.DiscardCard.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.STATION), 4, 2),

                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BREEDING_VALUE_5), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.GREEN_TEEPEE), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 3),

                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BREEDING_VALUE_5), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING), 5, 2)
        );
    }

    public boolean isEmpty() {
        return available.isEmpty();
    }

    public ObjectiveCard draw() {
        if (drawStack.isEmpty()) {
            throw new GWTException(GWTError.OBJECTIVE_CARD_NOT_AVAILABLE);
        }
        return drawStack.poll();
    }
}
