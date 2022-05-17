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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobMarketTest {

    @Nested
    class AddWorker {

        @Test
        void fill2Players() {
            JobMarket jobMarket = new JobMarket();

            for (int n = 0; n < 24; n++) {
                jobMarket.addWorker(Worker.COWBOY, 2);
            }

            assertThat(jobMarket.getCurrentRowIndex()).isEqualTo(12);
            assertThat(jobMarket.isClosed()).isTrue();
        }

        @Test
        void fill3Players() {
            JobMarket jobMarket = new JobMarket();

            for (int n = 0; n < 36; n++) {
                jobMarket.addWorker(Worker.COWBOY, 3);
            }

            assertThat(jobMarket.getCurrentRowIndex()).isEqualTo(12);
            assertThat(jobMarket.isClosed()).isTrue();
        }

        @Test
        void fill4Players() {
            JobMarket jobMarket = new JobMarket();

            for (int n = 0; n < 48; n++) {
                jobMarket.addWorker(Worker.COWBOY, 4);
            }

            assertThat(jobMarket.getCurrentRowIndex()).isEqualTo(12);
            assertThat(jobMarket.isClosed()).isTrue();
        }
    }

}
