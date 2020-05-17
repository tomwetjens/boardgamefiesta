package com.wetjens.gwt;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

class ActionStack implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Deque<PossibleAction> actions;

    ActionStack(Collection<PossibleAction> startActions) {
        this.actions = new LinkedList<>(startActions);
    }

    void push(Collection<? extends PossibleAction> possibleActions) {
        possibleActions.forEach(actions::addFirst);
    }

    void perform(Class<? extends Action> action) {
        PossibleAction element = check(action);

        element.perform(action);

        if (element.isFinal()) {
            actions.remove(element);
        }
    }

    boolean canPerform(Class<? extends Action> action) {
        return peek().canPerform(action);
    }

    Set<Class<? extends Action>> getPossibleActions() {
        return !actions.isEmpty() ? actions.peek().getPossibleActions() : Collections.emptySet();
    }

    private PossibleAction check(Class<? extends Action> action) {
        PossibleAction element = peek();

        if (!element.canPerform(action)) {
            throw new GWTException(GWTError.NOT_FIRST_ACTION);
        }

        return element;
    }

    PossibleAction peek() {
        if (actions.isEmpty()) {
            throw new GWTException(GWTError.NO_ACTIONS);
        }
        return actions.peek();
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

    public void skipAll() {
        while (!actions.isEmpty()) {
            skip();
        }
    }

    public void skip() {
        if (actions.isEmpty()) {
            throw new GWTException(GWTError.NO_ACTIONS);
        }

        actions.peek().skip();
        actions.poll();
    }

    public int size() {
        return actions.size();
    }

    void clear() {
        actions.clear();
    }
}
