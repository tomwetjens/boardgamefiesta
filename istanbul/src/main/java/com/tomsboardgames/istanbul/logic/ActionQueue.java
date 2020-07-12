package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.json.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionQueue {

    private final List<PossibleAction> anyTime;
    private final LinkedList<PossibleAction> queue;
    private PossibleAction current;

    ActionQueue() {
        queue = new LinkedList<>();
        anyTime = new LinkedList<>();
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("anyTime", serializer.fromCollection(anyTime, PossibleAction::serialize))
                .add("queue", serializer.fromCollection(queue, PossibleAction::serialize))
                .add("current", current != null ? factory.createObjectBuilder()
                        .add("anyTime", anyTime.indexOf(current))
                        .add("queue", queue.indexOf(current)) : null)
                .build();
    }

    static ActionQueue deserialize(JsonObject jsonObject) {
        var anyTime = jsonObject.getJsonArray("anyTime").stream()
                .map(JsonValue::asJsonObject)
                .map(PossibleAction::deserialize)
                .collect(Collectors.toList());

        var queue = jsonObject.getJsonArray("queue").stream()
                .map(JsonValue::asJsonObject)
                .map(PossibleAction::deserialize)
                .collect(Collectors.toCollection(LinkedList::new));

        var current = jsonObject.getJsonObject("current");

        return new ActionQueue(anyTime, queue, current != null
                ? current.getInt("anyTime") != -1
                ? anyTime.get(current.getInt("anyTime"))
                : queue.get(current.getInt("queue"))
                : null);
    }

    void perform(@NonNull Class<? extends Action> action) {
        if (current != null) {
            current.perform(action);

            if (current.isCompleted()) {
                if (!anyTime.remove(current)) {
                    this.queue.remove(current);
                }
                current = null;
            }
        } else {
            var possibleAction = getAnyTimeAction(action).orElse(queue.peek());

            if (possibleAction == null) {
                throw new IstanbulException(IstanbulError.NO_ACTION);
            }

            possibleAction.perform(action);

            if (possibleAction.isCompleted()) {
                if (!anyTime.remove(possibleAction)) {
                    this.queue.remove(possibleAction);
                }
            } else {
                current = possibleAction;
            }
        }
    }

    private Optional<PossibleAction> getAnyTimeAction(Class<? extends Action> action) {
        return anyTime.stream()
                .filter(possibleAction -> possibleAction.canPerform(action))
                .findAny();
    }

    public boolean canPerform(@NonNull Class<? extends Action> action) {
        if (current != null) {
            return current.canPerform(action);
        }
        return canPerformAnyTime(action) || canPerformInOrder(action);
    }

    private boolean canPerformInOrder(@NonNull Class<? extends Action> action) {
        var possibleAction = queue.peek();
        return possibleAction != null && possibleAction.canPerform(action);
    }

    private boolean canPerformAnyTime(@NonNull Class<? extends Action> action) {
        return getAnyTimeAction(action).isPresent();
    }

    public void addFirst(@NonNull PossibleAction possibleAction) {
        queue.addFirst(possibleAction);
    }

    public void addFirst(@NonNull Collection<PossibleAction> possibleActions) {
        queue.addAll(0, possibleActions);
    }

    public void skip() {
        if (current != null) {
            current.skip();
            current = null;
        }

        var possibleAction = queue.peek();

        if (possibleAction == null) {
            throw new IstanbulException(IstanbulError.NO_ACTION);
        }

        possibleAction.skip();
        queue.poll();
    }

    public void skipAll() {
        if (current != null) {
            current.skip();
        }

        PossibleAction possibleAction;
        while ((possibleAction = queue.peek()) != null) {
            possibleAction.skip();
            queue.poll();
        }

        anyTime.clear();
    }

    public boolean isEmpty() {
        return anyTime.isEmpty() && queue.isEmpty();
    }

    public Set<Class<? extends Action>> getPossibleActions() {
        if (current != null) {
            return current.getPossibleActions().collect(Collectors.toSet());
        }
        return Stream.concat(this.anyTime.stream(), this.queue.stream().limit(1))
                .flatMap(PossibleAction::getPossibleActions)
                .collect(Collectors.toSet());
    }

    void addAnyTime(@NonNull PossibleAction possibleAction) {
        this.anyTime.add(possibleAction);
    }

    public void clear() {
        queue.clear();
        anyTime.clear();
    }

    public Optional<PossibleAction> getCurrent() {
        return Optional.ofNullable(current);
    }

}
