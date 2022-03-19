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

package com.boardgamefiesta.gwt.view;

import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Elements of the first list will be compared, in order, to the corresponding element in the second list.
 * If one list is longer than the other list, remaining elements will be compared to <code>null</code>.
 *
 * @param <T>
 */
@RequiredArgsConstructor
class IterableComparator<T> implements Comparator<Iterable<T>> {

    private final Comparator<? super T> comparator;

    @Override
    public int compare(Iterable<T> o1, Iterable<T> o2) {
        Iterator<T> iterator1 = o1.iterator();
        Iterator<T> iterator2 = o2.iterator();

        while (iterator1.hasNext()) {
            T element1 = iterator1.next();
            T element2 = iterator2.hasNext() ? iterator2.next() : null;

            int result = comparator.compare(element1, element2);
            if (result != 0) {
                return result;
            }
        }

        while (iterator2.hasNext()) {
            T element2 = iterator2.next();

            int result = comparator.compare(null, element2);
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }
}
