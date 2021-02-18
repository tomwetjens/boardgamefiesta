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