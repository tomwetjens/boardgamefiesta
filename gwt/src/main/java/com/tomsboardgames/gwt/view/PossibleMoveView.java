package com.tomsboardgames.gwt.view;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.gwt.Location;
import com.tomsboardgames.gwt.PossibleMove;
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
        this.to = possibleMove.getSteps().get(possibleMove.getSteps().size() - 1).getName();
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
