package com.tomsboardgames.gwt.view;

import com.tomsboardgames.gwt.Building;
import com.tomsboardgames.gwt.PlayerBuilding;
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
