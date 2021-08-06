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

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KansasCitySupplyTest {

    private static List<KansasCitySupply.Tile> drawAllTiles(KansasCitySupply kansasCitySupply, int drawPileIndex) {
        return Stream.generate(() -> null)
                .map(i -> kansasCitySupply.draw(drawPileIndex))
                .takeWhile(Optional::isPresent)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Test
    void original() {
        var kansasCitySupply = KansasCitySupply.original(new Random(0));

        assertOriginal(kansasCitySupply);
    }

    private void assertOriginal(KansasCitySupply kansasCitySupply) {
        var tiles1 = drawAllTiles(kansasCitySupply, 0);
        assertThat(tiles1.size()).isEqualTo(35);
        assertThat(tiles1).filteredOn(tile -> tile.getHazard() != null).hasSize(18);
        hazardsByType(tiles1, HazardType.DROUGHT).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 2, 3, 3, 4, 4);
        hazardsByType(tiles1, HazardType.DROUGHT).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK, Hand.BLACK);
        hazardsByType(tiles1, HazardType.FLOOD).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 2, 3, 3, 4, 4);
        hazardsByType(tiles1, HazardType.FLOOD).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK, Hand.BLACK);
        hazardsByType(tiles1, HazardType.ROCKFALL).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 2, 3, 3, 4, 4);
        hazardsByType(tiles1, HazardType.ROCKFALL).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK, Hand.BLACK);
        assertThat(tiles1).filteredOn(tile -> tile.getTeepee() == Teepee.GREEN).hasSize(9);
        assertThat(tiles1).filteredOn(tile -> tile.getTeepee() == Teepee.BLUE).hasSize(8);

        var tiles2 = drawAllTiles(kansasCitySupply, 1);
        assertThat(tiles2.size()).isEqualTo(33);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.COWBOY).hasSize(11);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.CRAFTSMAN).hasSize(11);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.ENGINEER).hasSize(11);

        var tiles3 = drawAllTiles(kansasCitySupply, 2);
        assertThat(tiles3.size()).isEqualTo(26);
        assertThat(tiles3).filteredOn(tile -> tile.getTeepee() == Teepee.GREEN).hasSize(2);
        assertThat(tiles3).filteredOn(tile -> tile.getTeepee() == Teepee.BLUE).hasSize(3);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.COWBOY).hasSize(7);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.CRAFTSMAN).hasSize(7);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.ENGINEER).hasSize(7);
    }

    private AbstractListAssert<?, List<? extends Hazard>, Hazard, ObjectAssert<Hazard>> hazardsByType(List<KansasCitySupply.Tile> tiles1, HazardType hazardType) {
        return assertThat(tiles1).extracting(KansasCitySupply.Tile::getHazard).filteredOn(Objects::nonNull).filteredOn(hazard -> hazard.getType() == hazardType);
    }

    @Test
    void balanced2P() {
        var kansasCitySupply = KansasCitySupply.balanced(2, new Random(0));

        var tiles1 = drawAllTiles(kansasCitySupply, 0);
        assertThat(tiles1.size()).isEqualTo(23);
        assertThat(tiles1).filteredOn(tile -> tile.getHazard() != null).hasSize(12);
        hazardsByType(tiles1, HazardType.DROUGHT).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 3, 3, 4);
        hazardsByType(tiles1, HazardType.DROUGHT).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK);
        hazardsByType(tiles1, HazardType.FLOOD).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 3, 3, 4);
        hazardsByType(tiles1, HazardType.FLOOD).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK);
        hazardsByType(tiles1, HazardType.ROCKFALL).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 3, 3, 4);
        hazardsByType(tiles1, HazardType.ROCKFALL).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK);
        assertThat(tiles1).filteredOn(tile -> tile.getTeepee() == Teepee.GREEN).hasSize(6);
        assertThat(tiles1).filteredOn(tile -> tile.getTeepee() == Teepee.BLUE).hasSize(5);

        var tiles2 = drawAllTiles(kansasCitySupply, 1);
        assertThat(tiles2.size()).isEqualTo(18);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.COWBOY).hasSize(6);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.CRAFTSMAN).hasSize(6);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.ENGINEER).hasSize(6);

        var tiles3 = drawAllTiles(kansasCitySupply, 2);
        assertThat(tiles3.size()).isEqualTo(15);
        assertThat(tiles3).filteredOn(tile -> tile.getTeepee() == Teepee.GREEN).hasSize(1);
        assertThat(tiles3).filteredOn(tile -> tile.getTeepee() == Teepee.BLUE).hasSize(2);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.COWBOY).hasSize(4);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.CRAFTSMAN).hasSize(4);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.ENGINEER).hasSize(4);
    }

    @Test
    void balanced3P() {
        var kansasCitySupply = KansasCitySupply.balanced(3, new Random(0));

        var tiles1 = drawAllTiles(kansasCitySupply, 0);
        assertThat(tiles1.size()).isEqualTo(29);
        assertThat(tiles1).filteredOn(tile -> tile.getHazard() != null).hasSize(15);
        hazardsByType(tiles1, HazardType.DROUGHT).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 2, 3, 4, 4);
        hazardsByType(tiles1, HazardType.DROUGHT).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK, Hand.BLACK);
        hazardsByType(tiles1, HazardType.FLOOD).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 2, 3, 4, 4);
        hazardsByType(tiles1, HazardType.FLOOD).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK, Hand.BLACK);
        hazardsByType(tiles1, HazardType.ROCKFALL).extracting(Hazard::getPoints).containsExactlyInAnyOrder(2, 2, 3, 4, 4);
        hazardsByType(tiles1, HazardType.ROCKFALL).extracting(Hazard::getHand).containsExactlyInAnyOrder(Hand.GREEN, Hand.GREEN, Hand.GREEN, Hand.BLACK, Hand.BLACK);
        assertThat(tiles1).filteredOn(tile -> tile.getTeepee() == Teepee.GREEN).hasSize(7);
        assertThat(tiles1).filteredOn(tile -> tile.getTeepee() == Teepee.BLUE).hasSize(7);

        var tiles2 = drawAllTiles(kansasCitySupply, 1);
        assertThat(tiles2.size()).isEqualTo(24);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.COWBOY).hasSize(8);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.CRAFTSMAN).hasSize(8);
        assertThat(tiles2).filteredOn(tile -> tile.getWorker() == Worker.ENGINEER).hasSize(8);

        var tiles3 = drawAllTiles(kansasCitySupply, 2);
        assertThat(tiles3.size()).isEqualTo(22);
        assertThat(tiles3).filteredOn(tile -> tile.getTeepee() == Teepee.GREEN).hasSize(2);
        assertThat(tiles3).filteredOn(tile -> tile.getTeepee() == Teepee.BLUE).hasSize(2);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.COWBOY).hasSize(6);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.CRAFTSMAN).hasSize(6);
        assertThat(tiles3).filteredOn(tile -> tile.getWorker() == Worker.ENGINEER).hasSize(6);
    }

    @Test
    void balanced4P() {
        var kansasCitySupply = KansasCitySupply.balanced(4, new Random(0));

        assertOriginal(kansasCitySupply);
    }
}