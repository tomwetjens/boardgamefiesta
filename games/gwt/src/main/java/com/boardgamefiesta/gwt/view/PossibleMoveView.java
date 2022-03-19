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

package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.gwt.logic.Location;
import com.boardgamefiesta.gwt.logic.PossibleMove;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Value
// consider routes with exactly the same costs and fees to be equal, ignoring the actual steps
@EqualsAndHashCode(of = {"to", "cost", "playerFees"})
public class PossibleMoveView {

    String to;
    int cost;
    List<String> steps;
    List<String> route;
    List<PlayerFeeView> playerFees;

    public PossibleMoveView(PossibleMove possibleMove) {
        this.to = possibleMove.getTo().getName();
        this.cost = possibleMove.getCost();
        this.steps = possibleMove.getSteps().stream()
                .map(Location::getName)
                .collect(Collectors.toList());
        this.route = calculateRoute(possibleMove);
        this.playerFees = possibleMove.getPlayerFees().entrySet().stream()
                .map(entry -> new PlayerFeeView(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<String> calculateRoute(PossibleMove possibleMove) {
        var result = new LinkedList<Location>();

        Location current = possibleMove.getFrom().orElse(null);
        for (Location step : possibleMove.getSteps()) {
            if (current != null && !current.getNext().contains(step)) {
                // Fill in gap between
                result.addAll(current.routes(step)
                        // Only consider routes that fill in the empty gap
                        .filter(route -> route.stream()
                                .filter(location -> location != step)
                                .allMatch(Location::isEmpty))
                        // Since the locations in between are all empty, just take the shortest route
                        .min(Comparator.comparingInt(List::size))
                        .orElseThrow());
            } else {
                result.add(step);
            }

            current = step;
        }

        return result.stream().map(Location::getName).collect(Collectors.toList());
    }


    @Value
    public static class PlayerFeeView {
        PlayerView player;
        int fee;

        PlayerFeeView(Player player, int fee) {
            this.player = new PlayerView(player);
            this.fee = fee;
        }
    }
}
