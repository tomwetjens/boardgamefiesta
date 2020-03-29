package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

class ActionQueue {

    private final LinkedList<PossibleAction> queue;

    ActionQueue(Class<? extends Action>... startActions) {
        this.queue = Arrays.stream(startActions)
                .map(PossibleAction::of)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    void addFirst(Collection<? extends PossibleAction> possibleActions) {
        possibleActions.forEach(queue::addFirst);
    }

    public void add(PossibleAction possibleAction) {
        queue.add(possibleAction);
    }

    void perform(Class<? extends Action> action) {
        PossibleAction element = check(action);

        element.perform(action);

        if (element.isFinal()) {
            queue.remove(element);
        }
    }

    void skip(Class<? extends Action> action) {
        PossibleAction element = check(action);

        if (element.canSkip(action)) {
            throw new IllegalArgumentException("Not allowed to skip action");
        }

        element.skip(action);

        if (element.isFinal()) {
            queue.remove(element);
        }

        // Remove consecutive repeating occurrences
        PossibleAction next = queue.peek();
        while (next != null && next.equals(element)) {
            queue.poll();
            next = queue.peek();
        }
    }

    boolean canPerform(Class<? extends Action> action) {
        return first().canPerform(action);
    }

    Set<Class<? extends Action>> getPossibleActions() {
        return !queue.isEmpty() ? queue.peek().getPossibleActions() : Collections.emptySet();
    }

    private PossibleAction check(Class<? extends Action> action) {
        PossibleAction element = first();

        if (!element.canPerform(action)) {
            throw new IllegalArgumentException("Not first action");
        }

        return element;
    }

    PossibleAction first() {
        if (queue.isEmpty()) {
            throw new IllegalStateException("No actions");
        }
        return queue.peek();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
