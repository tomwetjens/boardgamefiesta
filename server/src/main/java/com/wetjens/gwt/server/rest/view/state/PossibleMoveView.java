package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerBuilding;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class PossibleMoveView {

    int cost;
    List<String> steps;
    List<PlayerFeeView> playerFees;

    public PossibleMoveView(int playerCount, List<Location> steps, Map<Player, User> userMap) {
        this.playerFees = steps.stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .flatMap(buildingLocation -> buildingLocation.getBuilding().stream())
                .filter(building -> building instanceof PlayerBuilding)
                .map(building -> (PlayerBuilding) building)
                .collect(Collectors.groupingBy(PlayerBuilding::getPlayer))
                .entrySet().stream()
                .map(entry -> new PlayerFeeView(new PlayerView(entry.getKey(), userMap.get(entry.getKey())), entry.getValue().stream()
                        .mapToInt(playerBuilding -> playerBuilding.getHand().getFee(playerCount))
                        .sum()))
                .collect(Collectors.toList());

        this.cost = steps.stream()
                .map(Location::getHand)
                .mapToInt(hand -> hand.getFee(playerCount))
                .sum();

        this.steps = steps.stream()
                .map(Location::getName)
                .collect(Collectors.toList());
    }

    @Value
    public static class PlayerFeeView {
        PlayerView player;
        int fee;
    }
}
