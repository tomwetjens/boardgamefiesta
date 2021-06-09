package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PossibleAction {

    public static PossibleAction optional(@NonNull Class<? extends Action> action) {
        return new Single(action, false);
    }

    public static PossibleAction optional(@NonNull PossibleAction possibleAction) {
        return new Any(Collections.singleton(possibleAction), null);
    }

    public static PossibleAction any(@NonNull Set<PossibleAction> actions) {
        return new Any(actions, null);
    }

    public static PossibleAction mandatory(@NonNull Class<? extends Action> action) {
        return new Single(action, true);
    }

    public static PossibleAction choice(@NonNull Set<PossibleAction> actions) {
        return new Choice(actions);
    }

    public static PossibleAction repeat(int atLeast, int atMost, @NonNull PossibleAction possibleAction) {
        return new Repeat(atLeast, atMost, possibleAction);
    }

    public static PossibleAction whenThen(@NonNull PossibleAction when, @NonNull PossibleAction then, int atLeast, int atMost) {
        return new WhenThen(when, then, atLeast, atMost);
    }

    abstract void perform(Class<? extends Action> action);

    abstract void skip();

    abstract boolean canPerform(Class<? extends Action> action);

    abstract boolean isCompleted();

    abstract Stream<Class<? extends Action>> getPossibleActions();

    protected abstract PossibleAction clone();

    abstract JsonObject serialize(JsonBuilderFactory factory);

    static PossibleAction deserialize(JsonObject jsonObject) {
        var any = jsonObject.getJsonObject("any");
        if (any != null) {
            return Any.deserialize(any);
        }

        var choice = jsonObject.getJsonObject("choice");
        if (choice != null) {
            return Choice.deserialize(choice);
        }

        var repeat = jsonObject.getJsonObject("repeat");
        if (repeat != null) {
            return Repeat.deserialize(repeat);
        }

        var whenThen = jsonObject.getJsonObject("whenThen");
        if (whenThen != null) {
            return WhenThen.deserialize(whenThen);
        }

        return Single.deserialize(jsonObject);
    }

    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Single extends PossibleAction {

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

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("action", action.getSimpleName())
                    .add("mandatory", mandatory)
                    .add("completed", completed)
                    .build();
        }

        @SuppressWarnings("unchecked")
        static Single deserialize(JsonObject jsonObject) {
            try {
                return new Single(
                        (Class<? extends Action>) Class.forName(Action.class.getName() + "$" + jsonObject.getString("action")),
                        jsonObject.getBoolean("mandatory"),
                        jsonObject.getBoolean("completed"));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @ToString
    private static class Any extends PossibleAction {

        private final Set<PossibleAction> possibleActions;

        private PossibleAction current;

        private Any(@NonNull Set<PossibleAction> possibleActions, PossibleAction current) {
            if (possibleActions.isEmpty()) {
                throw new IllegalArgumentException("Must contain at least 1 possible action");
            }
            this.possibleActions = new HashSet<>(possibleActions);
            this.current = current;
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (current == null) {
                current = possibleActions.stream()
                        .filter(possibleAction -> possibleAction.canPerform(action))
                        .findFirst()
                        .orElseThrow(() -> new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION));
            }

            current.perform(action);

            if (current.isCompleted()) {
                possibleActions.remove(this.current);
                current = null;
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
                    .collect(Collectors.toSet()), current);
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("any", factory.createObjectBuilder()
                            .add("possibleActions", JsonSerializer.forFactory(factory).fromCollection(possibleActions, PossibleAction::serialize))
                            .add("current", current != null ? current.serialize(factory) : null))
                    .build();
        }

        static Any deserialize(JsonObject jsonObject) {
            var current = jsonObject.getJsonObject("current");
            return new Any(jsonObject.getJsonArray("possibleActions").stream()
                    .map(JsonValue::asJsonObject)
                    .map(PossibleAction::deserialize)
                    .collect(Collectors.toSet()), current != null ? PossibleAction.deserialize(current) : null);
        }
    }

    @ToString
    private static class Choice extends PossibleAction {

        private final Set<PossibleAction> choices;

        private Choice(@NonNull Set<PossibleAction> choices) {
            this.choices = new HashSet<>(choices);
        }

        @Override
        void perform(Class<? extends Action> action) {
            var choice = choices.stream()
                    .filter(possibleAction -> possibleAction.canPerform(action))
                    .findFirst()
                    .orElseThrow(() -> new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION));

            choice.perform(action);

            if (choice.isCompleted()) {
                choices.clear();
            } else {
                // Choice has been made, others are not an option anymore
                choices.removeIf(possibleAction -> possibleAction != choice);
            }
        }

        @Override
        void skip() {
            throw new IstanbulException(IstanbulError.CANNOT_SKIP_ACTION);
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            return choices.stream().anyMatch(possibleAction -> possibleAction.canPerform(action));
        }

        @Override
        boolean isCompleted() {
            return choices.isEmpty();
        }

        @Override
        Stream<Class<? extends Action>> getPossibleActions() {
            return choices.stream().flatMap(PossibleAction::getPossibleActions);
        }

        @Override
        protected PossibleAction clone() {
            return new Choice(choices.stream()
                    .map(PossibleAction::clone)
                    .collect(Collectors.toSet()));
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("choice", factory.createObjectBuilder()
                            .add("choices", JsonSerializer.forFactory(factory).fromCollection(choices, PossibleAction::serialize)))
                    .build();
        }

        static Choice deserialize(JsonObject jsonObject) {
            return new Choice(jsonObject.getJsonArray("choices").stream()
                    .map(JsonValue::asJsonObject)
                    .map(PossibleAction::deserialize)
                    .collect(Collectors.toSet()));
        }
    }

    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Repeat extends PossibleAction {

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

            if (current.isCompleted()) {
                current = null;
                count++;
            }
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
            return current != null
                    ? current.getPossibleActions()
                    : count < atMost ? repeatingAction.getPossibleActions()
                    : Stream.empty();
        }

        @Override
        protected PossibleAction clone() {
            return new Repeat(atLeast, atMost, repeatingAction.clone(), current != null ? current.clone() : null, count);
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("repeat", factory.createObjectBuilder()
                            .add("atLeast", atLeast)
                            .add("atMost", atMost)
                            .add("repeatingAction", repeatingAction.serialize(factory))
                            .add("current", current != null ? current.serialize(factory) : null)
                            .add("count", count))
                    .build();
        }

        static Repeat deserialize(JsonObject jsonObject) {
            var current = jsonObject.getJsonObject("current");
            return new Repeat(
                    jsonObject.getInt("atLeast"),
                    jsonObject.getInt("atMost"),
                    PossibleAction.deserialize(jsonObject.getJsonObject("repeatingAction")),
                    current != null ? PossibleAction.deserialize(current) : null,
                    jsonObject.getInt("count"));
        }
    }

    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class WhenThen extends PossibleAction {

        private final PossibleAction when;
        private final PossibleAction then;
        private final int atLeast;
        private final int atMost;

        private int whens;
        private int thens;
        private PossibleAction current;

        private WhenThen(PossibleAction when, PossibleAction then, int atLeast, int atMost) {
            this.when = when;
            this.then = then;
            this.atLeast = atLeast;
            this.atMost = atMost;
        }

        @Override
        void perform(Class<? extends Action> action) {
            if (current == null) {
                if (then.canPerform(action) && thens < whens) {
                    // Start a "then"
                    current = then.clone();
                    thens++;
                } else if (when.canPerform(action) && whens < atMost) {
                    // Start a "when"
                    current = when.clone();
                    whens++;
                } else {
                    throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
                }
            }

            current.perform(action);

            if (current.isCompleted()) {
                current = null;
            }
        }

        @Override
        boolean canPerform(Class<? extends Action> action) {
            if (current != null) {
                // Must continue the current first
                return current.canPerform(action);
            }
            return (whens < atMost && when.canPerform(action)) || (whens >= atLeast && thens < whens && then.canPerform(action));
        }

        @Override
        void skip() {
            if (current != null) {
                current.skip();
                current = null;
            }

            if (whens < atLeast || thens < whens) {
                throw new IstanbulException(IstanbulError.CANNOT_SKIP_ACTION);
            }
        }

        @Override
        boolean isCompleted() {
            return current == null && whens == atMost && thens == whens;
        }

        @Override
        Stream<Class<? extends Action>> getPossibleActions() {
            if (current != null) {
                return current.getPossibleActions();
            }
            return Stream.concat(
                    whens < atMost ? when.getPossibleActions() : Stream.empty(),
                    thens < whens ? then.getPossibleActions() : Stream.empty());
        }

        @Override
        protected PossibleAction clone() {
            return new WhenThen(when.clone(), then.clone(), atLeast, atMost, whens, thens, current.clone());
        }

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("whenThen", factory.createObjectBuilder()
                            .add("when", when.serialize(factory))
                            .add("then", then.serialize(factory))
                            .add("atLeast", atLeast)
                            .add("atMost", atMost)
                            .add("whens", whens)
                            .add("thens", thens)
                            .add("current", current != null ? current.serialize(factory) : null))
                    .build();
        }

        static WhenThen deserialize(JsonObject jsonObject) {
            var current = jsonObject.getJsonObject("current");
            return new WhenThen(
                    PossibleAction.deserialize(jsonObject.getJsonObject("when")),
                    PossibleAction.deserialize(jsonObject.getJsonObject("then")),
                    jsonObject.getInt("atLeast"),
                    jsonObject.getInt("atMost"),
                    jsonObject.getInt("whens"),
                    jsonObject.getInt("thens"),
                    current != null ? PossibleAction.deserialize(current) : null);
        }
    }
}
