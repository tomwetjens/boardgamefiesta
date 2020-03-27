package com.wetjens.gwt;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ImmediateActions {

    private final List<ImmediateAction> actions;

    private ImmediateActions(List<ImmediateAction> actions) {
        this.actions = actions;
    }

    static ImmediateActions of(PossibleAction possibleAction) {
        return new ImmediateActions(Collections.singletonList(new ImmediateAction(possibleAction)));
    }

    static ImmediateActions none() {
        return new ImmediateActions(Collections.emptyList());
    }

    List<ImmediateAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public ImmediateActions andThen(PossibleAction possibleAction) {
        return new ImmediateActions(Stream.concat(actions.stream(), Stream.of(new ImmediateAction(possibleAction)))
                .collect(Collectors.toUnmodifiableList()));
    }

    private static final class ImmediateAction extends PossibleAction {

        private final PossibleAction possibleAction;

        private ImmediateAction(PossibleAction possibleAction) {
            this.possibleAction = possibleAction;
        }

        @Override
        public void perform(Class<? extends Action> action) {
            possibleAction.perform(action);
        }

        @Override
        public void skip(Class<? extends Action> action) {
            possibleAction.skip(action);
        }

        @Override
        public boolean isFinal() {
            return possibleAction.isFinal();
        }

        @Override
        public boolean canPerform(Class<? extends Action> action) {
            return possibleAction.canPerform(action);
        }

        @Override
        public boolean canSkip(Class<? extends Action> action) {
            return possibleAction.canSkip(action);
        }

        @Override
        public Set<Class<? extends Action>> getPossibleActions() {
            return possibleAction.getPossibleActions();
        }

        @Override
        public boolean isImmediate() {
            return true;
        }
    }
}
