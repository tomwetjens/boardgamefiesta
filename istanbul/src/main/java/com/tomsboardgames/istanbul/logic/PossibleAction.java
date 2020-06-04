package com.tomsboardgames.istanbul.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PossibleAction {

    public static PossibleAction optional(@NonNull Class<? extends Action> action) {
        return new Single(action, false);
    }

    public static PossibleAction optional(@NonNull PossibleAction possibleAction) {
        return new Any(Collections.singleton(possibleAction));
    }

    public static PossibleAction any(@NonNull Set<PossibleAction> actions) {
        return new Any(actions);
    }

    public static PossibleAction mandatory(@NonNull Class<? extends Action> action) {
        return new Single(action, true);
    }

    public static PossibleAction choice(@NonNull Set<Class<? extends Action>> actions) {
        return new Choice(actions);
    }

    public static PossibleAction repeat(int atLeast, int atMost, @NonNull PossibleAction possibleAction) {
        return new Repeat(atLeast, atMost, possibleAction);
    }

    public static PossibleAction whenThen(@NonNull PossibleAction when, @NonNull PossibleAction then) {
        return new WhenThen(when, then);
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

        private Single(@NonNull Class<? extends Action> action, boolean mandatory) {
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

        private Any(@NonNull Set<PossibleAction> possibleActions) {
            if (possibleActions.isEmpty()) {
                throw new IllegalArgumentException("Must contain at least 1 possible action");
            }
            this.possibleActions = new HashSet<>(possibleActions);
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (this.current == null) {
                this.current = possibleActions.stream()
                        .filter(possibleAction -> possibleAction.canPerform(action))
                        .findFirst()
                        .orElseThrow(() -> new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION));
            }

            this.current.perform(action);

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

        private Choice(@NonNull Set<Class<? extends Action>> choices) {
            if (choices.size() < 2) {
                throw new IllegalArgumentException("Must provide at least 2 choices");
            }
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

        private Repeat(int atLeast, int atMost, @NonNull PossibleAction repeatingAction) {
            if (atLeast < 0) {
                throw new IllegalArgumentException("At least must be > 0");
            }
            if (atMost < 1) {
                throw new IllegalArgumentException("At most must be > 1");
            }
            if (atLeast > atMost) {
                throw new IllegalArgumentException("At least must be <= at most");
            }

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

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class WhenThen extends PossibleAction {

        private final PossibleAction when;
        private final PossibleAction then;

        private final List<PossibleAction> thens;
        private PossibleAction currentThen;

        private WhenThen(PossibleAction when, PossibleAction then) {
            this.when = when;
            this.then = then;
            this.thens = new LinkedList<>();
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (currentThen != null) {
                // Continue the current "then" first
                currentThen.perform(action);
            } else if (then.canPerform(action)) {
                // Start a "then"
                currentThen = thens.get(0);
                currentThen.perform(action);
            } else {
                // Continue the "when"
                when.perform(action);
                thens.add(then.clone());
            }

            // If current is completed, remove it
            if (currentThen != null && currentThen.isCompleted()) {
                thens.remove(currentThen);
                currentThen = null;
            }
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            if (currentThen != null) {
                // Must continue the current "then" first
                return currentThen.canPerform(action);
            }
            return when.canPerform(action) || (thens.size() > 0 && then.canPerform(action));
        }

        @Override
        void skip() {
            if (currentThen != null) {
                currentThen.skip();
                currentThen = null;
            }

            when.skip();
        }

        @Override
        boolean isCompleted() {
            return thens.isEmpty() && when.isCompleted();
        }

        @Override
        Stream<Class<? extends Action>> getPossibleActions() {
            if (currentThen != null) {
                return currentThen.getPossibleActions();
            }
            return Stream.concat(when.getPossibleActions(), thens.isEmpty() ? Stream.empty()
                    : then.getPossibleActions());
        }

        @Override
        protected PossibleAction clone() {
            return new WhenThen(when.clone(), then.clone(), thens.stream()
                    .map(PossibleAction::clone)
                    .collect(Collectors.toList()), currentThen.clone());
        }
    }
}
