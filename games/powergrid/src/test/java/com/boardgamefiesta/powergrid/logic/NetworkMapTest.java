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

package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.powergrid.logic.map.City;
import com.boardgamefiesta.powergrid.logic.map.NetworkMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkMapTest {

    @Test
    void germany() {
        assertThat(NetworkMap.GERMANY.getAreas()).hasSize(6);
        assertThat(NetworkMap.GERMANY.getCities()).hasSize(42);
    }

    City flensburg = NetworkMap.GERMANY.getCity("Flensburg");
    City muenchen = NetworkMap.GERMANY.getCity("München");
    City duisburg = NetworkMap.GERMANY.getCity("Duisburg");
    City essen = NetworkMap.GERMANY.getCity("Essen");

    @Test
    void isReachable() {
        assertThat(NetworkMap.GERMANY.isReachable(flensburg, muenchen)).isTrue();
    }

    @Test
    void shortestPath() {
        assertThat(NetworkMap.GERMANY.shortestPath(flensburg, muenchen, NetworkMap.GERMANY.getAreas()).getCost()).isEqualTo(88);
        assertThat(NetworkMap.GERMANY.shortestPath(duisburg, essen, NetworkMap.GERMANY.getAreas()).getCost()).isEqualTo(0);
    }
}