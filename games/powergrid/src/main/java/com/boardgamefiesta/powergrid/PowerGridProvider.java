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

package com.boardgamefiesta.powergrid;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.powergrid.logic.Automa;
import com.boardgamefiesta.powergrid.logic.PowerGrid;
import com.boardgamefiesta.powergrid.logic.map.NetworkMap;
import com.boardgamefiesta.powergrid.view.ActionType;
import com.boardgamefiesta.powergrid.view.PowerGridView;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public class PowerGridProvider implements GameProvider<PowerGrid> {

    public static final String ID = "power-grid";

    private static final Duration DEFAULT_TIME_LIMIT = Duration.of(3, ChronoUnit.MINUTES);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return EnumSet.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.GREEN, PlayerColor.BLACK, PlayerColor.PURPLE);
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 6;
    }

    @Override
    public PowerGrid start(Set<Player> players, Options options, InGameEventListener eventListener, Random random) {
        return PowerGrid.start(players, NetworkMap.GERMANY, NetworkMap.GERMANY.randomAreas(players.size(), random), eventListener, random);
    }

    @Override
    public void executeAutoma(PowerGrid state, Player player, Random random) {
        new Automa().execute(state, player, random);
    }

    @Override
    public ActionMapper<PowerGrid> getActionMapper() {
        return ActionType::toAction;
    }

    @Override
    public ViewMapper<PowerGrid> getViewMapper() {
        return PowerGridView::new;
    }

    @Override
    public StateSerializer<PowerGrid> getStateSerializer() {
        return PowerGrid::serialize;
    }

    @Override
    public StateDeserializer<PowerGrid> getStateDeserializer() {
        return PowerGrid::deserialize;
    }

    @Override
    public boolean hasAutoma() {
        return true;
    }

}
