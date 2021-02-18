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
        actions = powerGrid.getActions().stream().map(ActionType::from).collect(Collectors.toSet());
        playerStates = powerGrid.getPlayerStates().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), entry -> new PlayerStateView(entry.getValue())));
        powerPlantMarket = new PowerPlantMarketView(powerGrid.getPowerPlantMarket());
        resourceMarket = new ResourceMarketView(powerGrid.getResourceMarket());
        auction = powerGrid.getAuction().map(AuctionView::new).orElse(null);
    }
}
