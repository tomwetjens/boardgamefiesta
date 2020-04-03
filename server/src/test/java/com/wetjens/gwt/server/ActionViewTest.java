package com.wetjens.gwt.server;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.wetjens.gwt.Action;

import static org.assertj.core.api.Assertions.*;

class ActionViewTest {

    @ParameterizedTest
    @MethodSource("allActions")
    void of(Class<? extends Action> action) {
        assertThat(ActionView.of(action)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ActionView.class)
    void naming(ActionView actionView) {
        assertThat(actionView.getAction().getSimpleName().toUpperCase())
                .describedAs("Enum constant " + actionView + " should match naming of action " + actionView.getAction().getSimpleName())
                .isEqualTo(actionView.name().replaceAll("[_]", ""));
    }

    static Stream<Arguments> allActions() {
        return Arrays.stream(Action.class.getDeclaredClasses())
                .filter(Action.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .map(Arguments::of);
    }
}
