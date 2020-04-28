package com.wetjens.gwt;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobMarketTest {

    @Nested
    class AddWorker {

        @Test
        void add() {
            JobMarket jobMarket = new JobMarket(4);

            for (int n = 0; n < 48; n++) {
                jobMarket.addWorker(Worker.COWBOY);
            }

            assertThat(jobMarket.getCurrentRowIndex()).isEqualTo(13);
            assertThat(jobMarket.isClosed()).isTrue();
        }
    }

}
