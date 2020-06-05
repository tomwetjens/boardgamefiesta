package com.tomsboardgames.istanbul.logic;

import lombok.NonNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActionQueue implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LinkedList<PossibleAction> inOrder = new LinkedList<>();
    private final List<Class<? extends Action>> anyTime = new LinkedList<>();

    void perform(@NonNull Class<? extends Action> action) {
        if (anyTime.remove(action)) {
            return;
        }

        var possibleAction = inOrder.peek();

        if (possibleAction == null) {
            throw new IstanbulException(IstanbulError.NO_ACTION);
        }

        possibleAction.perform(action);

        if (possibleAction.isCompleted()) {
            inOrder.remove(possibleAction);
        }
    }

    public boolean canPerform(@NonNull Class<? extends Action> action) {
        if (anyTime.contains(action)) {
            return true;
        }
        var possibleAction = inOrder.peek();
        return possibleAction != null && possibleAction.canPerform(action);
    }

    public void addFirst(@NonNull PossibleAction possibleAction) {
        inOrder.addFirst(possibleAction);
    }

    public void addFirst(@NonNull Collection<PossibleAction> possibleActions) {
        inOrder.addAll(0, possibleActions);
    }

    public void skip() {
        var possibleAction = inOrder.peek();

        if (possibleAction == null) {
            throw new IstanbulException(IstanbulError.NO_ACTION);
        }

        possibleAction.skip();

        if (possibleAction.isCompleted()) {
            inOrder.poll();
        }
    }

    public void skipAll() {
        anyTime.clear();

        PossibleAction possibleAction;
        while ((possibleAction = inOrder.peek()) != null) {
            possibleAction.skip();
            inOrder.poll();
        }
    }

    public boolean isEmpty() {
        return anyTime.isEmpty() && inOrder.isEmpty();
    }

    public Set<Class<? extends Action>> getPossibleActions() {
        return Stream.concat(this.anyTime.stream(), this.inOrder.stream().limit(1)
                .flatMap(PossibleAction::getPossibleActions))
                .collect(Collectors.toSet());
    }

    void addAnyTime(@NonNull Class<? extends Action> action) {
        this.anyTime.add(action);
    }
}
