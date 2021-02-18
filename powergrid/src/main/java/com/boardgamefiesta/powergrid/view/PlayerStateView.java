package com.boardgamefiesta.powergrid.view;

import com.boardgamefiesta.powergrid.logic.PlayerState;
import com.boardgamefiesta.powergrid.logic.PowerPlant;
import com.boardgamefiesta.powergrid.logic.ResourceType;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
public class PlayerStateView {

    int balance;
    List<PowerPlantStateView> powerPlants;

    public PlayerStateView(PlayerState playerState) {
        balance = playerState.getBalance();
        powerPlants = playerState.getPowerPlants().stream()
                .map(powerPlant -> new PowerPlantStateView(powerPlant, playerState.getResources(powerPlant)))
                .sorted()
                .collect(Collectors.toList());
    }

    @Value
    public static class PowerPlantStateView {

        PowerPlantView powerPlant;

        PowerPlantStateView(PowerPlant powerPlant, List<ResourceType> resources) {
            this.powerPlant = new PowerPlantView(powerPlant);
            // TODO
        }
    }
}
