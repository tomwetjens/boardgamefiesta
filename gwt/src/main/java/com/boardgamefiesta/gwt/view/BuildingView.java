package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Building;
import com.boardgamefiesta.gwt.logic.PlayerBuilding;
import lombok.Value;

@Value
public class BuildingView {

    String name;
    PlayerView player;

    BuildingView(Building building) {
        this.name = building.getName();
        this.player = building instanceof PlayerBuilding ? new PlayerView(((PlayerBuilding) building).getPlayer()) : null;
    }

}
