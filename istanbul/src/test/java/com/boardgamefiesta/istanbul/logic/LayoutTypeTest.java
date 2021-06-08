package com.boardgamefiesta.istanbul.logic;

import org.junit.jupiter.api.Test;

import java.util.Random;

class LayoutTypeTest {

    @Test
    void random() {
        var layout = LayoutType.RANDOM.createLayout(2, new Random());

        // TODO Assert The Fountain 7 has to be on one of the 4 Places in the middle of the grid.
        // TODO Assert The Black Market 8 and the Tea House 9 should have a distance from each other of at least 3 Places
    }
}