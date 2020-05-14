package com.wetjens.gwt;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CattleTypeTest {

    @Nested
    class Ordering {

        @Test
        void brownSwissBeforeAyshire() {
            var list = Arrays.asList(CattleType.AYRSHIRE, CattleType.BROWN_SWISS);
            Collections.sort(list);

            assertThat(list).containsExactly(CattleType.BROWN_SWISS, CattleType.AYRSHIRE);
        }
    }

}
