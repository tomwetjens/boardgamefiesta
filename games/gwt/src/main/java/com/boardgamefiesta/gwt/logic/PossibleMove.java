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

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class PossibleMove {

    Location from;
    List<Location> steps;
    int cost;
    Map<Player, Integer> playerFees;

    static PossibleMove fromTo(@NonNull Location from, @NonNull List<Location> steps, @NonNull Player player, int balance, int playerCount) {
        return new PossibleMove(from, steps,
                calculateCost(steps, player, balance, playerCount),
                calculatePlayerFees(steps, player, balance, playerCount));
    }

    static PossibleMove firstMove(@NonNull Location to) {
        return new PossibleMove(null, Collections.singletonList(to), 0, Collections.emptyMap());
    }

    private static int calculateCost(@NonNull List<Location> steps, @NonNull Player player, int balance, int playerCount) {
        return Math.min(balance, steps.stream()
                .filter(location -> !(location instanceof Location.BuildingLocation) || ((Location.BuildingLocation) location).getBuilding()
                        .filter(building -> building instanceof PlayerBuilding)
                        .map(building -> (PlayerBuilding) building)
                        .map(playerBuilding -> !playerBuilding.getPlayer().equals(player))
                        .orElse(false))
                .mapToInt(location -> location.getHand().getFee(playerCount))
                .sum());
    }

    private static Map<Player, Integer> calculatePlayerFees(List<Location> steps, Player player, int balance, int playerCount) {
        var remainingBalance = new AtomicInteger(balance);

        return steps.stream()
                .flatMap(location -> {
                    var fee = Math.min(remainingBalance.get(), location.getHand().getFee(playerCount));

                    if (location instanceof Location.BuildingLocation) {
                        var otherPlayerFee = ((Location.BuildingLocation) location).getBuilding()
                                .filter(building -> building instanceof PlayerBuilding)
                                .map(building -> (PlayerBuilding) building)
                                .filter(playerBuilding -> !playerBuilding.getPlayer().equals(player))
                                .filter(playerBuilding -> playerBuilding.getHand() != Hand.NONE)
                                .map(playerBuilding -> new PlayerFee(playerBuilding.getPlayer(), fee));

                        if (otherPlayerFee.isPresent()) {
                            remainingBalance.getAndAdd(-fee);
                        }

                        return otherPlayerFee.stream();
                    } else {
                        // hazard or teepee
                        remainingBalance.getAndAdd(-fee);
                    }
                    return Stream.empty();
                })
                .filter(playerFee -> playerFee.getFee() > 0)
                .collect(Collectors.groupingBy(PlayerFee::getPlayer)).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().mapToInt(PlayerFee::getFee).sum()));
    }

    public Optional<Location> getFrom() {
        return Optional.ofNullable(from);
    }

    public Location getTo() {
        return steps.get(steps.size() - 1);
    }

    public int getNumberOfSteps() {
        return steps.size();
    }

    @Value
    private static class PlayerFee {
        Player player;
        int fee;
    }
}
