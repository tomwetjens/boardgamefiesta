package com.wetjens.gwt.server.rest.view;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.PlayerBuilding;
import lombok.Value;

@Value
public class PossibleMoveView {

    int cost;
    List<TrailView.LocationView> steps;
    Map<String, Integer> playerFees;

    public PossibleMoveView(int playerCount, List<Location> steps) {
        this.playerFees = steps.stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .flatMap(buildingLocation -> buildingLocation.getBuilding().stream())
                .filter(building -> building instanceof PlayerBuilding)
                .map(building -> (PlayerBuilding) building)
                .collect(Collectors.groupingBy(PlayerBuilding::getPlayer))
                .entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), entry -> entry.getValue().stream()
                        .mapToInt(playerBuilding -> playerBuilding.getHand().getFee(playerCount))
                        .sum()));

        this.cost = steps.stream()
                .map(Location::getHand)
                .mapToInt(hand -> hand.getFee(playerCount))
                .sum();

        this.steps = steps.stream()
                .map(TrailView.LocationView::new)
                .collect(Collectors.toList());
    }
}
