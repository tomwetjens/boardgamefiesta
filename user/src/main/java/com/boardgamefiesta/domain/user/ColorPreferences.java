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

package com.boardgamefiesta.domain.user;

import com.boardgamefiesta.api.domain.PlayerColor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ColorPreferences {

    PlayerColor color1;

    PlayerColor color2;

    PlayerColor color3;

    public Optional<PlayerColor> getColor1() {
        return Optional.ofNullable(color1);
    }

    public Optional<PlayerColor> getColor2() {
        return Optional.ofNullable(color2);
    }

    public Optional<PlayerColor> getColor3() {
        return Optional.ofNullable(color3);
    }

    public void changeColors(PlayerColor color1, PlayerColor color2, PlayerColor color3) {
        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
    }

    public Optional<PlayerColor> pickPreferredColor(Set<PlayerColor> availableColors) {
        if (color1 != null && availableColors.contains(color1)) {
            return Optional.of(color1);
        } else if (color2 != null && availableColors.contains(color2)) {
            return Optional.of(color2);
        } else if (color3 != null && availableColors.contains(color3)) {
            return Optional.of(color3);
        } else {
            return Optional.empty();
        }
    }

}
