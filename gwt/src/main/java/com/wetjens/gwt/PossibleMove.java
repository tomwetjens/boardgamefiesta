package com.wetjens.gwt;

import com.wetjens.gwt.api.Player;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor
public class PossibleMove {

    Location from;
    Location to;
    List<Location> steps;
    int cost;
    Map<Player, Integer> playerFees;

    PossibleMove(Location from, Location to, List<Location> steps, Player player, int balance, int playerCount) {
        this.from = from;
        this.to = to;
        this.steps = steps;
        this.cost = Math.min(balance, steps.stream()
                .filter(location -> !(location instanceof Location.BuildingLocation) || ((Location.BuildingLocation) location).getBuilding()
                        .filter(building -> building instanceof PlayerBuilding)
                        .map(building -> (PlayerBuilding) building)
                        .map(playerBuilding -> playerBuilding.getPlayer() != player)
                        .orElse(false))
                .mapToInt(location -> location.getHand().getFee(playerCount))
                .sum());
        this.playerFees = calculatePlayerFees(steps, player, balance, playerCount);
    }

    private Map<Player, Integer> calculatePlayerFees(List<Location> steps, Player player, int balance, int playerCount) {
        var remainingBalance = new AtomicInteger(balance);
        return steps.stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .flatMap(buildingLocation -> buildingLocation.getBuilding().stream())
                .filter(building -> building instanceof PlayerBuilding)
                .map(building -> (PlayerBuilding) building)
                .filter(playerBuilding -> playerBuilding.getPlayer() != player)
                .filter(playerBuilding -> playerBuilding.getHand() != Hand.NONE)
                .map(playerBuilding -> {
                    var fee = Math.min(remainingBalance.get(), playerBuilding.getHand().getFee(playerCount));

                    remainingBalance.getAndAdd(-fee);

                    return new PlayerFee(playerBuilding.getPlayer(), fee);
                })
                .filter(playerFee -> playerFee.getFee() > 0)
                .collect(Collectors.groupingBy(PlayerFee::getPlayer)).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().mapToInt(PlayerFee::getFee).sum()));
    }

    @Value
    private class PlayerFee {
        Player player;
        int fee;
    }
}
