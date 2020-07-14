package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.gwt.logic.JobMarket;
import com.boardgamefiesta.gwt.logic.Worker;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobMarketTest {

    @Nested
    class AddWorker {

        @Test
        void fill2Players() {
            JobMarket jobMarket = new JobMarket(2);

            for (int n = 0; n < 24; n++) {
                jobMarket.addWorker(Worker.COWBOY);
            }

            assertThat(jobMarket.getCurrentRowIndex()).isEqualTo(12);
            assertThat(jobMarket.isClosed()).isTrue();
        }

        @Test
        void fill3Players() {
            JobMarket jobMarket = new JobMarket(3);

            for (int n = 0; n < 36; n++) {
                jobMarket.addWorker(Worker.COWBOY);
            }

            assertThat(jobMarket.getCurrentRowIndex()).isEqualTo(12);
            assertThat(jobMarket.isClosed()).isTrue();
        }

        @Test
        void fill4Players() {
            JobMarket jobMarket = new JobMarket(4);

            for (int n = 0; n < 48; n++) {
                jobMarket.addWorker(Worker.COWBOY);
            }

            assertThat(jobMarket.getCurrentRowIndex()).isEqualTo(12);
            assertThat(jobMarket.isClosed()).isTrue();
        }
    }

}
