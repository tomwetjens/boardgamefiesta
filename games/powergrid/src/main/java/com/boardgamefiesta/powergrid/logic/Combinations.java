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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Combinations {

    public static <T> Set<Set<T>> combinations(Set<T> s) {
        return null;
    }

    public static <T> Set<Set<T>> combinations(Set<T> s, int k) {
        if (k == 0) {
            return Collections.singleton(Collections.emptySet());
        }

        if (k == 1) {
            return s.stream().map(Collections::singleton).collect(Collectors.toSet());
        }

        return s.stream()
                .flatMap(elem ->
                        combinations(remove(s, elem), k - 1)
                        .stream()
                        .map(sc -> add(sc, elem)))
                .collect(Collectors.toSet());
    }

    public static <T> Set<List<T>> permutations(List<T> s) {
        if (s.size() == 1) {
            return Set.of(s);
        }
        return s.stream()
                .flatMap(elem -> permutations(remove(s, elem))
                        .stream()
                        .map(sc -> add(sc, elem)))
                .collect(Collectors.toSet());
    }

    private static <T> List<T> remove(List<T> list, T elem) {
        return list.stream()
                .filter(elem2 -> elem2 != elem)
                .collect(Collectors.toCollection(() -> new ArrayList<>(list.size() - 1)));
    }

    private static <T> List<T> add(List<T> list, T elem) {
        var ts = new ArrayList<T>(list.size() + 1);
        ts.addAll(list);
        ts.add(elem);
        return ts;
    }

    private static <T> Set<T> remove(Set<T> set, T elem) {
        return set.stream()
                .filter(elem2 -> elem2 != elem)
                .collect(Collectors.toSet());
    }

    private static <T> Set<T> add(Set<T> set, T elem) {
        var ts = new HashSet<T>(set);
        ts.add(elem);
        return ts;
    }

}
