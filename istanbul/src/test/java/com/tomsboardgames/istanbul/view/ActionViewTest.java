package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.Action;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ActionViewTest {

    @Test
    void completeness() {
        var actions = Arrays.stream(Action.class.getClasses())
                .filter(Action.class::isAssignableFrom)
                .map(clazz -> (Class<? extends Action>) clazz)
                .collect(Collectors.toList());

        assertThat(actions).allSatisfy(action -> {
            var view = ActionView.of(action);
            assertThat(view).isNotNull();
        });
    }
}