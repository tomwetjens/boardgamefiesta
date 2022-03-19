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

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.powergrid.logic.Phase;
import com.boardgamefiesta.powergrid.logic.PowerGrid;
import com.boardgamefiesta.powergrid.logic.map.Area;
import com.boardgamefiesta.powergrid.logic.map.City;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class PowerGridView {

    String map;
    Set<String> areas;
    List<String> playerOrder;
    int step;
    int round;
    Phase phase;
    Map<City, List<String>> cities;
    Set<ActionType> actions;
    Map<String, PlayerStateView> playerStates;
    PowerPlantMarketView powerPlantMarket;
    ResourceMarketView resourceMarket;
    AuctionView auction;

    public PowerGridView(@NonNull PowerGrid powerGrid, Player player) {
        map = powerGrid.getMap().getName();
        areas = powerGrid.getAreas().stream().map(Area::getName).collect(Collectors.toSet());
        playerOrder = powerGrid.getPlayerOrder().stream().map(Player::getName).collect(Collectors.toList());
        step = powerGrid.getStep();
        round = powerGrid.getRound();
        phase = powerGrid.getPhase();
        cities = powerGrid.getCities().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(Player::getName).collect(Collectors.toList())));
        actions = powerGrid.getActions(player).stream().map(ActionType::from).collect(Collectors.toSet());
        playerStates = powerGrid.getPlayerStates().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), entry -> new PlayerStateView(entry.getValue())));
        powerPlantMarket = new PowerPlantMarketView(powerGrid.getPowerPlantMarket());
        resourceMarket = new ResourceMarketView(powerGrid.getResourceMarket());
        auction = powerGrid.getAuction().map(AuctionView::new).orElse(null);
    }
}
