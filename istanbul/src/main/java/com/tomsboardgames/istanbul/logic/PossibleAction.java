package com.tomsboardgames.istanbul.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PossibleAction {

    public static PossibleAction optional(Class<? extends Action> action) {
        return new Single(action, false);
    }

    public static PossibleAction any(Set<PossibleAction> actions) {
        return new Any(actions);
    }

    public static PossibleAction mandatory(Class<? extends Action> action) {
        return new Single(action, true);
    }

    public static PossibleAction choice(Set<Class<? extends Action>> actions) {
        return new Choice(actions);
    }

    public static PossibleAction repeat(int atLeast, int atMost, PossibleAction possibleAction) {
        return new Repeat(atLeast, atMost, possibleAction);
    }

    abstract void perform(Class<? extends Action> action);

    abstract void skip();

    abstract boolean canPerform(Class<? extends Action> action);

    abstract boolean isCompleted();

    abstract Stream<Class<? extends Action>> getPossibleActions();

    protected abstract PossibleAction clone();

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Single extends PossibleAction implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Class<? extends Action> action;

        private final boolean mandatory;

        private boolean completed;

        private Single(Class<? extends Action> action, boolean mandatory) {
            this.action = action;
            this.mandatory = mandatory;
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (this.action != action || this.completed) {
                throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
            }

            this.completed = true;
        }

        @Override
        void skip() {
            if (this.mandatory) {
                throw new IstanbulException(IstanbulError.CANNOT_SKIP_ACTION);
            }

            this.completed = true;
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return this.action == action;
        }

        @Override
        boolean isCompleted() {
            return this.completed;
        }

        @Override
        Stream<Class<? extends Action>> getPossibleActions() {
            return completed ? Stream.empty() : Stream.of(action);
        }

        @Override
        protected PossibleAction clone() {
            return new Single(action, mandatory, completed);
        }
    }

    private static class Any extends PossibleAction implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Set<PossibleAction> possibleActions;

        private PossibleAction current;

        public Any(Set<PossibleAction> possibleActions) {
            this.possibleActions = new HashSet<>(possibleActions);
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (this.current != null) {
                this.current.perform(action);
            } else {
                this.current = possibleActions.stream()
                        .filter(possibleAction -> possibleAction.canPerform(action))
                        .findFirst()
                        .orElseThrow(() -> new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION));
            }

            if (this.current.isCompleted()) {
                this.possibleActions.remove(this.current);
                this.current = null;
            }
        }

        @Override
        void skip() {
            if (this.current != null) {
                this.current.skip();

                this.possibleActions.remove(this.current);
                this.current = null;
            } else {
                this.possibleActions.clear();
            }
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return possibleActions.stream().anyMatch(possibleAction -> possibleAction.canPerform(action));
        }

        @Override
        boolean isCompleted() {
            return possibleActions.isEmpty();
        }

        @Override
        Stream<Class<? extends Action>> getPossibleActions() {
            return this.possibleActions.stream().flatMap(PossibleAction::getPossibleActions);
        }

        @Override
        protected PossibleAction clone() {
            return new Any(possibleActions.stream()
                    .map(PossibleAction::clone)
                    .collect(Collectors.toSet()));
        }

    }

    private static class Choice extends PossibleAction implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Set<Class<? extends Action>> choices;

        Choice(Set<Class<? extends Action>> choices) {
            this.choices = new HashSet<>(choices);
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (!choices.contains(action)) {
                throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
            }

            // Choice has been made
            choices.clear();
        }

        @Override
        void skip() {
            throw new IstanbulException(IstanbulError.CANNOT_SKIP_ACTION);
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return choices.contains(action);
        }

        @Override
        boolean isCompleted() {
            return choices.isEmpty();
        }

        @Override
        Stream<Class<? extends Action>> getPossibleActions() {
            return choices.stream();
        }

        @Override
        protected PossibleAction clone() {
            return new Choice(choices);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Repeat extends PossibleAction implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int atLeast;
        private final int atMost;
        private final PossibleAction repeatingAction;

        private PossibleAction current;
        private int count;

        Repeat(int atLeast, int atMost, PossibleAction repeatingAction) {
            this.atLeast = atLeast;
            this.atMost = atMost;
            this.repeatingAction = repeatingAction;
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (count == atMost || !repeatingAction.canPerform(action)) {
                throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
            }

            if (current == null) {
                current = repeatingAction.clone();
            }

            current.perform(action);

            count++;
        }

        @Override
        void skip() {
            if (count < atLeast) {
                throw new IstanbulException(IstanbulError.CANNOT_SKIP_ACTION);
            }

            if (current != null) {
                current.skip();
            }

            count = atMost;
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return count < atMost && repeatingAction.canPerform(action);
        }

        @Override
        boolean isCompleted() {
            return count == atMost;
        }

        @Override
        Stream<Class<? extends Action>> getPossibleActions() {
            return current != null ? current.getPossibleActions() : repeatingAction.getPossibleActions();
        }

        @Override
        protected PossibleAction clone() {
            return new Repeat(atLeast, atMost, repeatingAction.clone(), current.clone(), count);
        }
    }
}
