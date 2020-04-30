package com.wetjens.gwt;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.junit.jupiter.*;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PossibleActionTest {

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
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
        }

        @Test
        void performCA() {
            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isTrue();
            assertThat(possibleAction.canPerform(B.class)).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            possibleAction.perform(A.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
        }

        @Test
        void performBC() {
            possibleAction.perform(B.class);
            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThat(possibleAction.canPerform(B.class)).isFalse();
            assertThat(possibleAction.canPerform(C.class)).isTrue();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
        }

        @Test
        void performCB() {
            possibleAction.perform(C.class);
            assertThat(possibleAction.isFinal()).isFalse();
            assertThat(possibleAction.canPerform(A.class)).isTrue();
            assertThat(possibleAction.canPerform(B.class)).isTrue();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            possibleAction.perform(B.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());

            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
        }

        @Test
        void performSingleAux() {
            possibleAction.perform(SingleAuxAction.class);
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThat(possibleAction.canPerform(B.class)).isFalse();
            assertThat(possibleAction.canPerform(C.class)).isFalse();
            assertThat(possibleAction.canPerform(SingleAuxAction.class)).isFalse();
            assertThatThrownBy(() -> possibleAction.perform(A.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
            assertThatThrownBy(() -> possibleAction.perform(B.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
            assertThatThrownBy(() -> possibleAction.perform(C.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
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
            assertThatThrownBy(() -> possibleAction.perform(SingleAuxAction.class)).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
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
            assertThatThrownBy(() -> possibleAction.skip()).hasMessage(GWTError.CANNOT_SKIP_ACTION.toString());
        }
    }

    @Nested
    class WhenThen {

        @Test
        void skip() {
            PossibleAction possibleAction = PossibleAction.whenThen(0, 3, A.class, B.class);
            possibleAction.skip();
        }

        @Test
        void skipAfterThen() {
            PossibleAction possibleAction = PossibleAction.whenThen(0, 3, A.class, B.class);
            assertThat(possibleAction.canPerform(A.class)).isTrue();
            assertThat(possibleAction.canPerform(B.class)).isFalse();

            possibleAction.skip();
            assertThat(possibleAction.isFinal()).isTrue();
            assertThat(possibleAction.canPerform(A.class)).isFalse();
            assertThat(possibleAction.canPerform(B.class)).isFalse();
        }

        @Test
        void skipThenNotAllowed() {
            PossibleAction possibleAction = PossibleAction.whenThen(0, 3, A.class, B.class);
            possibleAction.perform(A.class);

            assertThatThrownBy(possibleAction::skip).isInstanceOf(RuntimeException.class);
        }

        @Test
        void skipAtLeastNotMet() {
            PossibleAction possibleAction = PossibleAction.whenThen(1, 3, A.class, B.class);

            assertThatThrownBy(possibleAction::skip).isInstanceOf(RuntimeException.class);
        }

        @Test
        void firstWhensThenThens() {
            PossibleAction possibleAction = PossibleAction.whenThen(0, 3, A.class, B.class);

            possibleAction.perform(A.class);
            possibleAction.perform(A.class);
            possibleAction.perform(A.class);

            possibleAction.perform(B.class);
            possibleAction.perform(B.class);
            possibleAction.perform(B.class);
        }

        @Test
        void whenThenAlternating() {
            PossibleAction possibleAction = PossibleAction.whenThen(0, 3, A.class, B.class);

            possibleAction.perform(A.class);
            possibleAction.perform(B.class);

            possibleAction.perform(A.class);
            possibleAction.perform(B.class);

            possibleAction.perform(A.class);
            possibleAction.perform(B.class);
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
