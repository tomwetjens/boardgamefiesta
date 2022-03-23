/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.domain.automa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomaRequest {

    private static final Jsonb JSONB = JsonbBuilder.create();

    String tableId;
    String playerId;

    public static AutomaRequest fromJSON(String jsonString) {
        return JSONB.fromJson(jsonString, AutomaRequest.class);
    }

    public String toJSON() {
        return JSONB.toJson(this);
    }

}
