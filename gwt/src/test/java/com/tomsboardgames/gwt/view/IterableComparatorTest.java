package com.tomsboardgames.gwt.view;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
