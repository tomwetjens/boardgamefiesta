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

package com.boardgamefiesta.api.domain;

import lombok.Builder;
import lombok.Singular;

 import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Builder(toBuilder = true)
public class Stats {

    @Singular
    private final Map<String, Object> values;

    public Set<String> keys() {
        return values.keySet();
    }

    public Optional<Object> value(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }
}
