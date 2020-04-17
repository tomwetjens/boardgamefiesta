package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Building;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerBuilding;
import lombok.Value;

@Value
public class BuildingView {

    private String name;
    private Player player;

    BuildingView(Building building) {
        this.name = building.getName();
        this.player = building instanceof PlayerBuilding ? ((PlayerBuilding) building).getPlayer() : null;
    }

}
