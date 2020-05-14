package com.wetjens.gwt;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static org.assertj.core.api.Assertions.*;

class ActionTest {

    @ParameterizedTest
    @MethodSource("allActions")
    void accessible(Class<? extends Action> action) {
        assertThat(action).isPublic();
        assertThat(action.getConstructors()).isNotEmpty();
        assertThat(Modifier.isAbstract(action.getModifiers())).isFalse();
    }

    static Stream<Arguments> allActions() {
        return Arrays.stream(Action.class.getDeclaredClasses())
                .filter(Action.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .map(Arguments::of);
    }
}
