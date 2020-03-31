package com.wetjens.gwt;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.junit.jupiter.*;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PossibleActionTest {

    private static final String NOT_ALLOWED = "Not an allowed action";

    @Nested
    class NeutralBuildingActions {

        private PossibleAction possibleAction;

        @BeforeEach
        void setUp() {
            possibleAction = PossibleAction.optional(PossibleAction.choice(PossibleAction.any(PossibleAction.choice(A.class, B.class), C.class), SingleAuxAction.class));

            assertThat(possibleAction.isFinal()).isFalse();

            assertThat(possibleAction.canPerform(A.class)).isTrue();
            assertThat(possibleAction.canPerform(B.class)).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isTrue();
            assertThat(possibleAction.canPerform(D.class)).isFalse();
        }

        @Test
        void performAC() {
            possibleAction.perform(A.class);
            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThat(possibleAction.canPerform(B.class)).isFalse();
            assertThat(possibleAction.canPerform(C.class)).isTrue();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(NOT_ALLOWED);
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(NOT_ALLOWED);

            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(NOT_ALLOWED);

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(NOT_ALLOWED);
        }

        @Test
        void performCA() {
            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isTrue();
            assertThat(possibleAction.canPerform(B.class)).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(NOT_ALLOWED);

            possibleAction.perform(A.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(NOT_ALLOWED);
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(NOT_ALLOWED);

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(NOT_ALLOWED);
        }

        @Test
        void performBC() {
            possibleAction.perform(B.class);
            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThat(possibleAction.canPerform(B.class)).isFalse();
            assertThat(possibleAction.canPerform(C.class)).isTrue();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(NOT_ALLOWED);
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(NOT_ALLOWED);

            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(NOT_ALLOWED);

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(NOT_ALLOWED);
        }

        @Test
        void performCB() {
            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isTrue();
            assertThat(possibleAction.canPerform(B.class)).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(NOT_ALLOWED);

            possibleAction.perform(B.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(NOT_ALLOWED);
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(NOT_ALLOWED);

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(NOT_ALLOWED);
        }

        @Test
        void performSingleAux() {
            possibleAction.perform(SingleAuxAction.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThat(possibleAction.canPerform(B.class)).isFalse();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(NOT_ALLOWED);
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(NOT_ALLOWED);
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(NOT_ALLOWED);
            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(NOT_ALLOWED);
        }

        @Test
        void skip() {
            // When
            possibleAction.skip();

            // Then
            assertThat(possibleAction.isFinal()).isTrue();
        }

        @Test
        void skipAfterA() {
            // Given
            possibleAction.perform(A.class);

            // When
            possibleAction.skip();

            // Then
            assertThat(possibleAction.isFinal()).isTrue();
        }

        @Test
        void skipAfterB() {
            // Given
            possibleAction.perform(B.class);

            // When
            possibleAction.skip();

            // Then
            assertThat(possibleAction.isFinal()).isTrue();
        }

        @Test
        void skipAfterC() {
            // Given
            possibleAction.perform(C.class);

            // When
            possibleAction.skip();

            // Then
            assertThat(possibleAction.isFinal()).isTrue();
        }
    }

    @Nested
    class HazardOrOtherPlayersBuildingActions {

        private PossibleAction possibleAction;

        @BeforeEach
        void setUp() {
            possibleAction = PossibleAction.optional(SingleAuxAction.class);

            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isTrue();
        }

        @Test
        void perform() {
            possibleAction.perform(SingleAuxAction.class);

            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(NOT_ALLOWED);
        }

        @Test
        void skip() {
            // When
            possibleAction.skip();

            // Then
            assertThat(possibleAction.isFinal()).isTrue();
        }
    }

    @Nested
    class Mandatory {

        private PossibleAction possibleAction;

        @BeforeEach
        void setUp() {
            possibleAction = PossibleAction.mandatory(A.class);

            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isTrue();
        }

        @Test
        void perform() {
            possibleAction.perform(A.class);

            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
        }

        @Test
        void skip() {
            assertThatThrownBy(() -> possibleAction.skip()).hasMessage("Not allowed to skip action");
        }
    }

    abstract class A extends Action {
    }

    abstract class B extends Action {
    }

    abstract class C extends Action {
    }

    abstract class D extends Action {
    }

    abstract class E extends Action {
    }

    abstract class SingleAuxAction extends Action {
    }
}
