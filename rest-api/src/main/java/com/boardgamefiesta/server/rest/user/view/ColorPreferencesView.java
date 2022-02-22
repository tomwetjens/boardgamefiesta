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

package com.boardgamefiesta.server.rest.user.view;

import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.domain.user.ColorPreferences;
import com.boardgamefiesta.domain.user.TurnBasedPreferences;
import lombok.Value;

@Value
public class ColorPreferencesView {

    PlayerColor color1;
    PlayerColor color2;
    PlayerColor color3;

    public ColorPreferencesView(ColorPreferences colorPreferences) {
        color1 = colorPreferences.getColor1().orElse(null);
        color2 = colorPreferences.getColor2().orElse(null);
        color3 = colorPreferences.getColor3().orElse(null);
    }
}
