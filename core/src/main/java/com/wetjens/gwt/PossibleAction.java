package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class PossibleAction {

    /**
     * Player MUST perform the action and cannot skip it.
     */
    static PossibleAction mandatory(Class<? extends Action> action) {
        return new Mandatory(action);
    }

    /**
     * Player MAY perform the action or skip it.
     */
    static PossibleAction optional(Class<? extends Action> action) {
        return any(action);
    }

    /**
     * Player MAY perform the action or skip it.
     */
    static PossibleAction optional(PossibleAction possibleAction) {
        return any(possibleAction);
    }

    /**
     * Player MAY perform none, a single, some or all of the options in ANY order.
     */
    static PossibleAction any(Class<? extends Action>... actions) {
        return any(Arrays.asList(actions));
    }

    /**
     * Player MAY perform none, a single, some or all of the options in ANY order.
     */
    static PossibleAction any(Collection<Class<? extends Action>> actions) {
        return new Any(actions.stream()
                .map(PossibleAction::mandatory)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    /**
     * Player MAY perform none, a single, some or all of the options in ANY order.
     */
    static PossibleAction any(PossibleAction possibleAction, Class<? extends Action>... actions) {
        return new Any(Stream.concat(
                Stream.of(possibleAction),
                Arrays.stream(actions).map(PossibleAction::mandatory))
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    /**
     * Player MUST perform EXACTLY ONE of the options.
     */
    static PossibleAction choice(Class<? extends Action>... actions) {
        return choice(Arrays.asList(actions));
    }

    /**
     * Player MUST perform EXACTLY ONE of the options.
     */
    static PossibleAction choice(Collection<Class<? extends Action>> actions) {
        return new Choice(actions.stream()
                .map(PossibleAction::optional)
                .collect(Collectors.toCollection(HashSet::new)));
    }

    /**
     * Player MUST perform EXACTLY ONE of the options.
     */
    static PossibleAction choice(PossibleAction possibleAction, Class<? extends Action>... actions) {
        return new Choice(Stream.concat(Stream.of(possibleAction), Arrays.stream(actions)
                .map(PossibleAction::optional))
                .collect(Collectors.toCollection(HashSet::new)));
    }

    abstract void perform(Class<? extends Action> action);

    abstract void skip();

    abstract boolean isFinal();

    abstract boolean isImmediate();

    abstract boolean canPerform(Class<? extends Action> action);

    abstract Set<Class<? extends Action>> getPossibleActions();

    private static final class Mandatory extends PossibleAction {

        private Class<? extends Action> action;

        private Mandatory(Class<? extends Action> action) {
            this.action = action;
        }

        @Override
        void perform(Class<? extends Action> action) {
            this.action = null;
        }

        @Override
        void skip() {
            throw new IllegalArgumentException("Not allowed to skip action");
        }

        @Override
        boolean isFinal() {
            return action == null;
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return action != null && action.equals(this.action);
        }

        @Override
        boolean isImmediate() {
            return false;
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            return action != null ? Collections.singleton(action) : Collections.emptySet();
        }
    }

    private static final class Any extends PossibleAction {

        private final List<PossibleAction> actions;

        private Any(List<PossibleAction> actions) {
            this.actions = actions;
        }

        @Override
        void perform(Class<? extends Action> action) {
            PossibleAction element = check(action);

            element.perform(action);

            if (element.isFinal()) {
                actions.remove(element);
            }
        }

        @Override
        void skip() {
            actions.clear();
        }

        private PossibleAction check(Class<? extends Action> action) {
            return actions.stream()
                    .filter(fa -> fa.canPerform(action))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Not an allowed action"));
        }

        @Override
        boolean isFinal() {
            return actions.isEmpty();
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return actions.stream().anyMatch(fa -> fa.canPerform(action));
        }

        @Override
        boolean isImmediate() {
            return false;
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            return actions.stream()
                    .flatMap(action -> action.getPossibleActions().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private static final class Choice extends PossibleAction {

        private final Set<PossibleAction> actions;

        private Choice(Set<PossibleAction> actions) {
            this.actions = actions;
        }

        @Override
        void perform(Class<? extends Action> action) {
            PossibleAction element = check(action);

            element.perform(action);

            // Choice was made, other choices are no longer allowed
            if (element.isFinal()) {
                // Choice is also fully performed, so nothing left here to do
                actions.clear();
            } else {
                // Choice was made, but still has actions inside it, other choices are no longer allowed
                actions.removeIf(a -> a != element);
            }
        }

        @Override
        void skip() {
            throw new IllegalArgumentException("Must make a choice");
        }

        private PossibleAction check(Class<? extends Action> action) {
            return actions.stream()
                    .filter(fa -> fa.canPerform(action))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Not an allowed action"));
        }

        @Override
        boolean isFinal() {
            return actions.isEmpty();
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return actions.stream().anyMatch(fa -> fa.canPerform(action));
        }

        @Override
        boolean isImmediate() {
            return false;
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            // List all choices as possible. When one is performed, the others are removed
            return actions.stream()
                    .flatMap(action -> action.getPossibleActions().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
