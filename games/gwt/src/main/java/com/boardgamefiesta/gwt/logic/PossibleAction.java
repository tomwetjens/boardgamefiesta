/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
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
    @SafeVarargs
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
    static PossibleAction any(Stream<PossibleAction> actions) {
        return new Any(actions.collect(Collectors.toCollection(ArrayList::new)));
    }

    /**
     * Player MAY perform none, a single, some or all of the options in ANY order.
     */
    @SafeVarargs
    static PossibleAction any(PossibleAction possibleAction, Class<? extends Action>... actions) {
        return new Any(Stream.concat(
                Stream.of(possibleAction),
                Arrays.stream(actions).map(PossibleAction::mandatory))
                .collect(Collectors.toCollection(ArrayList::new)));
    }
    static PossibleAction repeat(int atLeast, int atMost, Class<? extends Action> action) {
        return new Repeat(atLeast, atMost, PossibleAction.optional(action), null);
    }

    /**
     * Player MUST perform EXACTLY ONE of the options.
     */
    @SafeVarargs
    static PossibleAction choice(Class<? extends Action>... actions) {
        return new Choice(Arrays.stream(actions)
                .map(PossibleAction::optional)
                .collect(Collectors.toList()));
    }

    /**
     * Player MUST perform EXACTLY ONE of the options.
     */
    static PossibleAction choice(Collection<PossibleAction> actions) {
        return new Choice(new ArrayList<>(actions));
    }

    /**
     * Player MUST perform EXACTLY ONE of the options.
     */
    @SafeVarargs
    static PossibleAction choice(PossibleAction possibleAction, Class<? extends Action>... actions) {
        return new Choice(Stream.concat(Stream.of(possibleAction), Arrays.stream(actions)
                .map(PossibleAction::optional))
                .collect(Collectors.toList()));
    }

    static PossibleAction deserialize(JsonObject jsonObject) {
        if (jsonObject.containsKey("mandatory")) {
            return Mandatory.deserialize(jsonObject.getJsonObject("mandatory"));
        }

        if (jsonObject.containsKey("any")) {
            return Any.deserialize(jsonObject.getJsonObject("any"));
        }

        if (jsonObject.containsKey("choice")) {
            return Choice.deserialize(jsonObject.getJsonObject("choice"));
        }

        if (jsonObject.containsKey("whenThen")) {
            return WhenThen.deserialize(jsonObject.getJsonObject("whenThen"));
        }

        return Repeat.deserialize(jsonObject.getJsonObject("repeat"));
    }

    abstract JsonObject serialize(JsonBuilderFactory factory);

    /**
     * Makes a copy.
     */
    public abstract PossibleAction clone();

    /**
     * Performs an action from this possible action.
     * E.g. if it is a choice, it picks the choice, or if it is a repeating action it counts one, or if it's a set, it removes the action.
     */
    abstract void perform(Class<? extends Action> action);

    /**
     * Skips (all) actions in this possible action.
     */
    abstract void skip();

    /**
     * Determines if the action is completed and can be removed from the stack.
     */
    abstract boolean isFinal();

    /**
     * Determines if the action can be performed.
     */
    abstract boolean canPerform(Class<? extends Action> action);

    abstract boolean canSkip();

    /**
     * Returns all possible actions that a player can perform next.
     * E.g. if it is a choice, all possible choices, or if it is a repeating action the action that can be performed once more.
     */
    abstract Set<Class<? extends Action>> getPossibleActions();

    private static final class Mandatory extends PossibleAction {

        private Class<? extends Action> action;

        private Mandatory(Class<? extends Action> action) {
            this.action = action;
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("mandatory", factory.createObjectBuilder()
                            .add("action", Action.serializeClass(action)))
                    .build();
        }

        static Mandatory deserialize(JsonObject jsonObject) {
            return new Mandatory(Action.deserializeClass(jsonObject.getString("action")));
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (this.action != action) {
                throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
            }

            this.action = null;
        }

        @Override
        void skip() {
            throw new GWTException(GWTError.CANNOT_SKIP_ACTION);
        }

        @Override
        boolean isFinal() {
            return action == null;
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return action != null && action == this.action;
        }

        @Override
        boolean canSkip() {
            return false;
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            return action != null ? Collections.singleton(action) : Collections.emptySet();
        }

        @Override
        public PossibleAction clone() {
            return new Mandatory(action);
        }

        @Override
        public String toString() {
            return action.getSimpleName();
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Any extends PossibleAction {

        private final List<PossibleAction> actions;

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("any", factory.createObjectBuilder()
                            .add("actions", JsonSerializer.forFactory(factory).fromCollection(actions, PossibleAction::serialize)))
                    .build();
        }

        static Any deserialize(JsonObject jsonObject) {
            var actions = jsonObject.getJsonArray("actions").stream()
                    .map(JsonValue::asJsonObject)
                    .map(PossibleAction::deserialize)
                    .collect(Collectors.toList());
            return new Any(actions);
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
                    .orElseThrow(() -> new GWTException(GWTError.CANNOT_PERFORM_ACTION));
        }

        @Override
        boolean isFinal() {
            return actions.isEmpty();
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return actions.stream().anyMatch(possibleAction -> possibleAction.canPerform(action));
        }

        @Override
        boolean canSkip() {
            return true;
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            return actions.stream()
                    .flatMap(action -> action.getPossibleActions().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public PossibleAction clone() {
            return new Any(actions.stream().map(PossibleAction::clone).collect(Collectors.toList()));
        }

        @Override
        public String toString() {
            return "Any(actions=" + actions + ")";
        }
    }

    private static final class Choice extends PossibleAction {

        private final List<PossibleAction> actions;

        private Choice(List<PossibleAction> actions) {
            this.actions = actions;
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("choice", factory.createObjectBuilder()
                            .add("actions", JsonSerializer.forFactory(factory).fromCollection(actions, PossibleAction::serialize)))
                    .build();
        }

        static Choice deserialize(JsonObject jsonObject) {
            return new Choice(jsonObject.getJsonArray("actions").stream()
                    .map(JsonValue::asJsonObject)
                    .map(PossibleAction::deserialize)
                    .collect(Collectors.toList()));
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
            if (actions.size() > 1) {
                throw new GWTException(GWTError.MUST_CHOOSE_ACTION);
            } else if (!actions.isEmpty()) {
                var action = actions.iterator().next();

                action.skip();

                if (action.isFinal()) {
                    actions.clear();
                }
            }
        }

        private PossibleAction check(Class<? extends Action> action) {
            return actions.stream()
                    .filter(fa -> fa.canPerform(action))
                    .findAny()
                    .orElseThrow(() -> new GWTException(GWTError.CANNOT_PERFORM_ACTION));
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
        boolean canSkip() {
            return actions.isEmpty() || (actions.size() == 1 && actions.get(0).canSkip());
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            // List all choices as possible. When one is performed, the others are removed
            return actions.stream()
                    .flatMap(action -> action.getPossibleActions().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public PossibleAction clone() {
            return new Choice(actions.stream().map(PossibleAction::clone).collect(Collectors.toList()));
        }
    }

    private static final class WhenThen extends Repeat {

        private final Class<? extends Action> when;
        private final Class<? extends Action> then;

        private int thens;

        private WhenThen(int atLeast, int atMost, Class<? extends Action> when, Class<? extends Action> then, int thens) {
            super(atLeast, atMost, PossibleAction.optional(when), null);
            this.when = when;
            this.then = then;
            this.thens = thens;
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("whenThen", factory.createObjectBuilder()
                            .add("when", Action.serializeClass(when))
                            .add("then", Action.serializeClass(then))
                            .add("thens", thens)
                            .add("atLeast", atLeast)
                            .add("atMost", atMost))
                    .build();
        }

        static Repeat deserialize(JsonObject jsonObject) {
            return new WhenThen(jsonObject.getInt("atLeast"),
                    jsonObject.getInt("atMost"),
                    Action.deserializeClass(jsonObject.getString("when")),
                    Action.deserializeClass(jsonObject.getString("then")),
                    jsonObject.getInt("thens"));
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (this.when == action) {
                super.perform(action);

                thens++;
            } else if (action == then) {
                if (thens == 0) {
                    throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
                }

                thens--;
            }
        }

        @Override
        void skip() {
            if (thens > 0) {
                throw new GWTException(GWTError.CANNOT_SKIP_ACTION);
            }

            super.skip();
        }

        @Override
        boolean isFinal() {
            return thens == 0 && super.isFinal();
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            if (action == then) {
                return thens > 0;
            } else {
                return super.canPerform(action);
            }
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            if (thens > 0) {
                return Stream.concat(
                        super.getPossibleActions().stream(),
                        Stream.of(then)
                ).collect(Collectors.toSet());
            }
            return super.getPossibleActions();
        }

        @Override
        public PossibleAction clone() {
            return new WhenThen(atLeast, atMost, when, then, thens);
        }
    }

    private static class Repeat extends PossibleAction {

        protected final PossibleAction repeatingAction;

        private PossibleAction current;
        protected int atLeast;
        protected int atMost;

        private Repeat(int atLeast, int atMost, PossibleAction action, PossibleAction current) {
            this.atLeast = atLeast;
            this.atMost = atMost;
            this.repeatingAction = action;
            this.current = current;
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("repeat", factory.createObjectBuilder()
                            .add("repeatingAction", repeatingAction.serialize(factory))
                            .add("current", current != null ? current.serialize(factory) : null)
                            .add("atLeast", atLeast)
                            .add("atMost", atMost))
                    .build();
        }

        static Repeat deserialize(JsonObject jsonObject) {
            if (jsonObject.containsKey("repeatingAction")) {
                var current = jsonObject.get("current");
                return new Repeat(jsonObject.getInt("atLeast"),
                        jsonObject.getInt("atMost"),
                        PossibleAction.deserialize(jsonObject.getJsonObject("repeatingAction")),
                        current != null && current != JsonValue.NULL ? PossibleAction.deserialize(current.asJsonObject()) : null);
            } else {
                // Deprecated
                return new Repeat(jsonObject.getInt("atLeast"),
                        jsonObject.getInt("atMost"),
                        PossibleAction.optional(Action.deserializeClass(jsonObject.getString("action"))),
                        null);
            }
        }

        @Override
        public PossibleAction clone() {
            return new Repeat(atLeast, atMost, repeatingAction, current);
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (current == null) {
                if (atMost == 0) {
                    throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
                }

                atLeast = Math.max(0, atLeast - 1);
                atMost--;

                current = repeatingAction.clone();
            }

            current.perform(action);

            if (current.isFinal()) {
                current = null;
            }
        }

        @Override
        void skip() {
            if (current != null) {
                current.skip();
                current = null;
            }

            if (atLeast > 0) {
                throw new GWTException(GWTError.CANNOT_SKIP_ACTION);
            }

            atMost = 0;
        }

        @Override
        boolean isFinal() {
            return current == null && atMost == 0;
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return current != null ? current.canPerform(action) : (repeatingAction.canPerform(action) && atMost > 0);
        }

        @Override
        boolean canSkip() {
            return (current != null && current.canSkip()) || atLeast == 0;
        }

        @Override
        Set<Class<? extends Action>> getPossibleActions() {
            return current != null ? current.getPossibleActions()
                    : (atMost > 0 ? repeatingAction.getPossibleActions() : Collections.emptySet());
        }
    }
}
