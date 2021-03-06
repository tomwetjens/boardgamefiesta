/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.gwt.logic.Action;
import com.boardgamefiesta.gwt.logic.GWTError;
import com.boardgamefiesta.gwt.logic.GWTException;
import com.boardgamefiesta.gwt.logic.PossibleAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
    class Repeat {

        @Test
        void skip() {
            PossibleAction possibleAction = PossibleAction.repeat(0, 1, A.class);

            possibleAction.skip();

            assertThat(possibleAction.isFinal()).isTrue();
        }

        @Test
        void skipAtLeastNotMet() {
            PossibleAction possibleAction = PossibleAction.repeat(1, 1, A.class);

            assertThatThrownBy(possibleAction::skip).isInstanceOf(GWTException.class).hasMessage(GWTError.CANNOT_SKIP_ACTION.toString());
        }

        @Test
        void atLeast1() {
            PossibleAction possibleAction = PossibleAction.repeat(1, 3, A.class);

            possibleAction.perform(A.class);
            possibleAction.perform(A.class);
            possibleAction.perform(A.class);

            assertThat(possibleAction.isFinal()).isTrue();
        }

        @Test
        void atLeast0() {
            PossibleAction possibleAction = PossibleAction.repeat(0, 3, A.class);

            possibleAction.perform(A.class);
            possibleAction.perform(A.class);
            possibleAction.perform(A.class);

            assertThat(possibleAction.isFinal()).isTrue();
        }

        @Test
        void atMost() {
            PossibleAction possibleAction = PossibleAction.repeat(0, 2, A.class);

            possibleAction.perform(A.class);
            possibleAction.perform(A.class);

            assertThatThrownBy(() -> possibleAction.perform(A.class)).isInstanceOf(GWTException.class).hasMessage(GWTError.CANNOT_PERFORM_ACTION.toString());
        }
    }

    @Nested
    class Any {

        @Mock
        PossibleAction a;

        @Mock
        PossibleAction b;

        @Test
        void shouldSkipOnlyCurrent() {
// TODO
        }

        @Test
        void shouldSkipAllIfNoCurrent() {
// TODO
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
