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

package com.boardgamefiesta.api.spi;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.*;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;

import java.util.Random;
import java.util.Set;

public interface GameProvider<T extends State> {

    String getId();

    int getMinNumberOfPlayers();

    int getMaxNumberOfPlayers();

    Set<PlayerColor> getSupportedColors();

    T start(Set<Player> players, Options options, InGameEventListener eventListener, Random random);

    StateSerializer<T> getStateSerializer();

    StateDeserializer<T> getStateDeserializer();

    ActionMapper<T> getActionMapper();

    ViewMapper<T> getViewMapper();

    void executeAutoma(T state, Player player, Random random);

    boolean hasAutoma();


}