package com.boardgamefiesta.gwt.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ImmediateActions {

    private final List<PossibleAction> actions;

    private ImmediateActions(List<PossibleAction> actions) {
        this.actions = actions;
    }

    static ImmediateActions of(PossibleAction possibleAction) {
        return new ImmediateActions(Arrays.asList(possibleAction));
    }

    static ImmediateActions none() {
        return new ImmediateActions(Collections.emptyList());
    }

    List<PossibleAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    ImmediateActions andThen(PossibleAction possibleAction) {
        return new ImmediateActions(Stream.concat(actions.stream(), Stream.of(possibleAction))
                .collect(Collectors.toUnmodifiableList()));
    }

    ImmediateActions andThen(ImmediateActions other) {
        return new ImmediateActions(Stream.concat(actions.stream(), other.actions.stream())
                .collect(Collectors.toUnmodifiableList()));
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }

}
