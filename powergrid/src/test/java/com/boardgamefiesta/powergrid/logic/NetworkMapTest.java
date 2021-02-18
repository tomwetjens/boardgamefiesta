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