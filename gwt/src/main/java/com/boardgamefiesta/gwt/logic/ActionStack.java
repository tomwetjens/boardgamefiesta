package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ActionStack {

    private final Deque<PossibleAction> actions;
    private final Deque<PossibleAction> immediateActions;

    static ActionStack initial(Collection<PossibleAction> startActions) {
        return new ActionStack(new LinkedList<>(startActions), new LinkedList<>());
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("actions", JsonSerializer.forFactory(factory).fromCollection(actions, PossibleAction::serialize))
                .add("immediateActions", JsonSerializer.forFactory(factory).fromCollection(immediateActions, PossibleAction::serialize))
                .build();
    }

    static ActionStack deserialize(JsonObject jsonObject) {
        return new ActionStack(
                jsonObject.getJsonArray("actions").stream()
                        .map(JsonValue::asJsonObject)
                        .map(PossibleAction::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                !jsonObject.containsKey("immediateActions") ? new LinkedList<>()
                        : jsonObject.getJsonArray("immediateActions").stream()
                        .map(JsonValue::asJsonObject)
                        .map(PossibleAction::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)));
    }

    void perform(Class<? extends Action> action) {
        PossibleAction element = check(action);

        element.perform(action);

        if (element.isFinal()) {
            if (immediateActions.isEmpty()) {
                actions.remove(element);
            } else {
                immediateActions.remove(element);
            }
        }
    }

    boolean canPerform(Class<? extends Action> action) {
        return !isEmpty() && peek().canPerform(action);
    }

    Set<Class<? extends Action>> getPossibleActions() {
        return !immediateActions.isEmpty()
                ? immediateActions.peek().getPossibleActions()
                : !actions.isEmpty()
                ? actions.peek().getPossibleActions()
                : Collections.emptySet();
    }

    private PossibleAction check(Class<? extends Action> action) {
        PossibleAction element = peek();

        if (!element.canPerform(action)) {
            throw new GWTException(GWTError.NOT_FIRST_ACTION);
        }

        return element;
    }

    private PossibleAction peek() {
        if (immediateActions.isEmpty()) {
            if (actions.isEmpty()) {
                throw new GWTException(GWTError.NO_ACTIONS);
            }
            return actions.peek();
        }
        return immediateActions.peek();
    }

    public boolean isEmpty() {
        return actions.isEmpty() && immediateActions.isEmpty();
    }

    public void skipAll() {
        skipAll(immediateActions);
        skipAll(actions);
    }

    private void skipAll(Deque<PossibleAction> stack) {
        while (!stack.isEmpty()) {
            skip(stack);
        }
    }

    public void skip() {
        if (!immediateActions.isEmpty()) {
            skip(immediateActions);
        } else {
            skip(actions);
        }
    }

    private void skip(Deque<PossibleAction> stack) {
        if (stack.isEmpty()) {
            throw new GWTException(GWTError.NO_ACTIONS);
        }

        var possibleAction = stack.peek();

        possibleAction.skip();

        if (possibleAction.isFinal()) {
            stack.poll();
        }
    }

    public int size() {
        return actions.size() + immediateActions.size();
    }

    void clear() {
        actions.clear();
        immediateActions.clear();
    }

    boolean hasImmediate() {
        return !immediateActions.isEmpty();
    }

    void addImmediateActions(ImmediateActions immediateActions) {
        // Pushes on top of stack, but keeps relative order.
        for (int i = immediateActions.getActions().size() - 1; i >= 0; i--) {
            this.immediateActions.addFirst(immediateActions.getActions().get(i));
        }
    }

    void addActions(List<PossibleAction> actions) {
        // Pushes on top of stack, but keeps relative order.
        for (int i = actions.size() - 1; i >= 0; i--) {
            addAction(actions.get(i));
        }
    }

    void addAction(PossibleAction action) {
        actions.addFirst(action);
    }
}
