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
