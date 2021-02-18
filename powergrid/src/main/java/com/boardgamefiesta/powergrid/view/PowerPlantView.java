package com.boardgamefiesta.powergrid.view;

import com.boardgamefiesta.powergrid.logic.PowerPlant;
import com.boardgamefiesta.powergrid.logic.ResourceType;
import lombok.Value;

import java.util.Set;

@Value
public class PowerPlantView {

    PowerPlant name;
    int cost;
    int requires;
    Set<ResourceType> consumes;
    int powers;

    PowerPlantView(PowerPlant powerPlant) {
       name = powerPlant;
       cost = powerPlant.getCost();
       requires = powerPlant.getRequires();
       consumes = powerPlant.getConsumes();
       powers = powerPlant.getPowers();
    }

}
