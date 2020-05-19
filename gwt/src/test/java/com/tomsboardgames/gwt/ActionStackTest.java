package com.tomsboardgames.gwt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActionStackTest {

    @Test
    void building4B() {
        var actionStack = new ActionStack(List.of(PossibleAction.any(
                PossibleAction.whenThen(0, 3, Action.DrawCard.class, Action.DiscardCard.class),
                Action.Move3Forward.class)));

        actionStack.perform(Action.DrawCard.class);
        actionStack.perform(Action.DrawCard.class);
        actionStack.perform(Action.DiscardCard.class);
        assertThatThrownBy(actionStack::skip).hasMessage(GWTError.CANNOT_SKIP_ACTION.name());
        actionStack.perform(Action.DiscardCard.class);

        assertThat(actionStack.getPossibleActions()).containsOnly(Action.DrawCard.class);
        actionStack.skip();

        assertThat(actionStack.getPossibleActions()).containsOnly(Action.Move3Forward.class);
        actionStack.perform(Action.Move3Forward.class);

        assertThat(actionStack.isEmpty()).isTrue();
    }
}