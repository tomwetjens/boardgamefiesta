package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.json.JsonSerializer;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;

class ActionStack {

    private final Deque<PossibleAction> actions;

    ActionStack(Collection<PossibleAction> startActions) {
        this.actions = new LinkedList<>(startActions);
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("actions", JsonSerializer.forFactory(factory).fromCollection(actions, PossibleAction::serialize))
                .build();
    }

    static ActionStack deserialize(JsonObject jsonObject) {
        return new ActionStack(jsonObject.getJsonArray("actions").stream()
                .map(JsonValue::asJsonObject)
                .map(PossibleAction::deserialize)
                .collect(Collectors.toList()));
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

        var possibleAction = actions.peek();

        possibleAction.skip();

        if (possibleAction.isFinal()) {
            actions.poll();
        }
    }

    public int size() {
        return actions.size();
    }

    void clear() {
        actions.clear();
    }

}
