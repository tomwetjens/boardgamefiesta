package com.wetjens.gwt.server.rest.view;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class IterableComparatorTest {

    @Test
    void sameLength() {
        IterableComparator<Integer> iterableComparator = new IterableComparator<>(Comparator.naturalOrder());

        assertThat(iterableComparator.compare(List.of(1,2), List.of(1,2))).isEqualTo(0);
        assertThat(iterableComparator.compare(List.of(2,1), List.of(1,2))).isEqualTo(1);
        assertThat(iterableComparator.compare(List.of(1,2), List.of(2,1))).isEqualTo(-1);
    }

    @Test
    void firstLonger() {
        IterableComparator<Integer> iterableComparator = new IterableComparator<>(Comparator.nullsLast(Comparator.naturalOrder()));

        assertThat(iterableComparator.compare(List.of(1,2), List.of(1))).isEqualTo(-1);
        assertThat(iterableComparator.compare(List.of(2,1), List.of(1))).isEqualTo(1);
        assertThat(iterableComparator.compare(List.of(1,2), List.of(2))).isEqualTo(-1);
    }

    @Test
    void secondLonger() {
        IterableComparator<Integer> iterableComparator = new IterableComparator<>(Comparator.nullsLast(Comparator.naturalOrder()));

        assertThat(iterableComparator.compare(List.of(1), List.of(1,2))).isEqualTo(1);
        assertThat(iterableComparator.compare(List.of(1), List.of(2,1))).isEqualTo(-1);
        assertThat(iterableComparator.compare(List.of(2), List.of(1))).isEqualTo(1);
    }

}
