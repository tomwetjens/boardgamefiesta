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

package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.api.domain.Player;

import java.util.Comparator;
import java.util.Random;
import java.util.stream.Collectors;

public class Automa {
    public void execute(PowerGrid state, Player player, Random random) {
        var actions = state.getActions(player);

        var playerState = state.getPlayerStates().get(player);
        if (actions.contains(Action.StartAuction.class)) {
            var shouldStartAuction = state.getRound() == 1 || random.nextFloat() <= 0.5;

            if (shouldStartAuction) {
                var afforablePowerPlants = state.getPowerPlantMarket().getActual().stream()
                        .filter(p -> p.getCost() <= playerState.getBalance())
                        .collect(Collectors.toList());

                if (!afforablePowerPlants.isEmpty()) {
                    var powerPlant = afforablePowerPlants.get(random.nextInt(afforablePowerPlants.size()));

                    state.perform(player, new Action.StartAuction(powerPlant), random);
                } else {
                    state.skip(player, random);
                }
            } else {
                state.skip(player, random);
            }
        } else if (actions.contains(Action.PlaceBid.class)) {
            var auction = state.getAuction().orElseThrow();
            var balance = playerState.getBalance();
            var bidToPlace = Math.min(balance, auction.getMinPlaceBid() + random.nextInt(auction.getMinPlaceBid() / 4));
            var shouldPlaceBid = bidToPlace <= balance
                    && random.nextFloat() <= 0.5;

            if (shouldPlaceBid) {
                state.perform(player, new Action.PlaceBid(bidToPlace), random);
            } else {
                state.skip(player, random);
            }
        } else if (actions.contains(Action.RemovePowerPlant.class)) {
            var powerPlant = playerState.getPowerPlants().stream()
                    .min(Comparator.comparingInt(PowerPlant::getCost))
                    .orElseThrow();

            state.perform(player, new Action.RemovePowerPlant(powerPlant), random);
        } else if (actions.contains(Action.BuyResource.class)) {
            var powerPlants = playerState.getPowerPlants();

            var powerPlantsThatNeedResources = powerPlants.stream()
                    .filter(p -> p.getRequires() > playerState.getResources(p).size())
                    .collect(Collectors.toSet());

            throw new IllegalStateException("Not implemented action: " + actions);
        } else if (actions.contains(Action.ConnectCity.class)) {
            throw new IllegalStateException("Not implemented action: " + actions);
        } else if (actions.contains(Action.ProducePower.class)) {
            throw new IllegalStateException("Not implemented action: " + actions);
        } else {
            throw new IllegalStateException("Unsupported action: " + actions);
        }
    }
}
