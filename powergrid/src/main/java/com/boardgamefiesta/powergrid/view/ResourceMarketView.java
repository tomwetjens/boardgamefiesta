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

package com.boardgamefiesta.powergrid.view;

import com.boardgamefiesta.powergrid.logic.ResourceMarket;
import com.boardgamefiesta.powergrid.logic.ResourceType;
import lombok.Value;

import java.util.Map;

@Value
public class ResourceMarketView {

    Map<ResourceType, Integer> available;

    public ResourceMarketView(ResourceMarket resourceMarket) {
        available = Map.of(
                ResourceType.COAL, resourceMarket.getAvailable(ResourceType.COAL),
                ResourceType.OIL, resourceMarket.getAvailable(ResourceType.OIL),
                ResourceType.BIO_MASS, resourceMarket.getAvailable(ResourceType.BIO_MASS),
                ResourceType.URANIUM, resourceMarket.getAvailable(ResourceType.URANIUM)
        );
    }
}
