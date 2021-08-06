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

import com.boardgamefiesta.powergrid.logic.Combinations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CombinationsTest {

    @Test
    void a() {
        assertThat(Combinations.combinations(Set.of(1, 2, 3), 2))
                .containsExactlyInAnyOrder(
                        Set.of(1, 2),
                        Set.of(1, 3),
                        Set.of(2, 3)
                );
    }

    @Test
    void combinationsB() {
        assertThat(Combinations.combinations(Set.of(1, 2, 3, 4), 2))
                .containsExactlyInAnyOrder(
                        Set.of(1, 2),
                        Set.of(1, 3),
                        Set.of(1, 4),
                        Set.of(2, 3),
                        Set.of(2, 4),
                        Set.of(3, 4)
                );
    }

    @Test
    void b() {
        assertThat(Combinations.combinations(Set.of(1, 2, 3), 3))
                .containsExactlyInAnyOrder(
                        Set.of(1, 2, 3)
                );
    }

    @Test
    void c() {
        assertThat(Combinations.permutations(List.of(1, 2, 3)))
                .containsExactlyInAnyOrder(
                        List.of(1, 2, 3),
                        List.of(1, 3, 2),
                        List.of(3, 2, 1),
                        List.of(3, 1, 2),
                        List.of(2, 1, 3),
                        List.of(2, 3, 1)
                );
    }
}