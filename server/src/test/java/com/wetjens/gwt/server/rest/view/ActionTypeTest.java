package com.wetjens.gwt.server.rest.view;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import com.wetjens.gwt.server.rest.view.state.ActionType;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.wetjens.gwt.Action;

import static org.assertj.core.api.Assertions.*;

class ActionTypeTest {

    @ParameterizedTest
    @MethodSource("allActions")
    void of(Class<? extends Action> action) {
        assertThat(ActionType.of(action)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ActionType.class)
    void naming(ActionType actionType) {
        assertThat(actionType.getAction().getSimpleName().toUpperCase())
                .describedAs("Enum constant " + actionType + " should match naming of action " + actionType.getAction().getSimpleName())
                .isEqualTo(actionType.name().replaceAll("[_]", ""));
    }

    static Stream<Arguments> allActions() {
        return Arrays.stream(Action.class.getDeclaredClasses())
                .filter(Action.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .map(Arguments::of);
    }
}
