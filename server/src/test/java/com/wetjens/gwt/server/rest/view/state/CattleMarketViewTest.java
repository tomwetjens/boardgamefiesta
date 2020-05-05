package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.CattleType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CattleMarketViewTest {

    @Nested
    class Ordering {

        @Test
        void brownSwissBeforeAyshire() {
            var ayshire = CattleCardView.builder()
                    .type(CattleType.AYRSHIRE)
                    .breedingValue(3)
                    .points(3)
                    .build();
            var brownSwiss = CattleCardView.builder()
                    .type(CattleType.BROWN_SWISS)
                    .breedingValue(3)
                    .points(2)
                    .build();

            var list = Arrays.asList(ayshire, brownSwiss);
            Collections.sort(list);

            assertThat(list).containsExactly(brownSwiss, ayshire);
        }
    }

}
