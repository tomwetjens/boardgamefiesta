package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor
public class PossibleMove {

    Location from;
    Location to;
    List<Location> steps;
    int cost;
    Map<Player, Integer> playerFees;

    PossibleMove(Location from, Location to, List<Location> steps, Player currentPlayer, int playerCount) {
        this.from = from;
        this.to = to;
        this.steps = steps;
        this.cost = steps.stream()
                .filter(location -> !(location instanceof Location.BuildingLocation) || ((Location.BuildingLocation) location).getBuilding()
                        .filter(building -> building instanceof PlayerBuilding)
                        .map(building -> (PlayerBuilding) building)
                        .map(playerBuilding -> playerBuilding.getPlayer() != currentPlayer)
                        .orElse(false))
                .mapToInt(location -> location.getHand().getFee(playerCount))
                .sum();
        this.playerFees = steps.stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .flatMap(buildingLocation -> buildingLocation.getBuilding().stream())
                .filter(building -> building instanceof PlayerBuilding)
                .map(building -> (PlayerBuilding) building)
                .filter(playerBuilding -> playerBuilding.getPlayer() != currentPlayer)
                .filter(playerBuilding -> playerBuilding.getHand() != Hand.NONE)
                .collect(Collectors.groupingBy(PlayerBuilding::getPlayer))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .mapToInt(playerBuilding -> playerBuilding.getHand().getFee(playerCount))
                        .sum()));
    }

}
