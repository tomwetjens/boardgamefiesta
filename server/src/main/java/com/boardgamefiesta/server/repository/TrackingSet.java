package com.boardgamefiesta.server.repository;

import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@link Set} that tracks elements that were added or removed, until {@link #flush()} is called.
 *
 * @param <T> Type of element.
 */
class TrackingSet<T> extends SetWrapper<T> {

    @Getter
    private final Set<T> added;
    @Getter
    private final Set<T> removed;

    public TrackingSet() {
        super(new HashSet<>());

        added = new HashSet<>();
        removed = new HashSet<>();
    }

    void flush() {
        added.clear();
        removed.clear();
    }

    Stream<T> getNotAddedOrRemoved() {
        return stream().filter(e -> !added.contains(e) && !removed.contains(e));
    }

    @Override
    public boolean add(T e) {
        added.add(e);
        removed.remove(e);
        return super.add(e);
    }

    @Override
    public boolean remove(Object o) {
        added.remove(o);
        removed.add((T) o);
        return super.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        added.addAll(c);
        removed.removeAll(c);
        return super.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        added.retainAll(c);
        super.forEach(e -> {
            if (!c.contains(e)) {
                removed.add(e);
            }
        });
        return super.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        added.removeAll(c);
        removed.addAll((Collection<? extends T>) c);
        return super.removeAll(c);
    }

    @Override
    public void clear() {
        added.clear();
        removed.addAll(this);
        super.clear();
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        super.forEach(e -> {
            if (filter.test(e)) {
                removed.add(e);
                added.remove(e);
            }
        });
        return super.removeIf(filter);
    }

}
