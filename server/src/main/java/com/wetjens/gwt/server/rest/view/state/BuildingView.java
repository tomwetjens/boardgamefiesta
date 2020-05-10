package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Building;
import com.wetjens.gwt.PlayerBuilding;
import lombok.Value;

@Value
public class BuildingView {

    private String name;
    private PlayerView player;

    BuildingView(Building building) {
        this.name = building.getName();
        this.player = building instanceof PlayerBuilding ? new PlayerView(((PlayerBuilding) building).getPlayer()) : null;
    }

}
