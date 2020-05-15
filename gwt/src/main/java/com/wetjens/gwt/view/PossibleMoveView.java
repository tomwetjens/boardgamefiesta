package com.wetjens.gwt.view;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.PossibleMove;
import com.wetjens.gwt.api.Player;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
// consider routes with exactly the same costs and fees to be equal, ignoring the actual steps
@EqualsAndHashCode(of = {"to", "cost", "playerFees"})
public class PossibleMoveView {

    String to;
    int cost;
    List<String> steps;
    List<PlayerFeeView> playerFees;

    public PossibleMoveView(PossibleMove possibleMove) {
        this.to = possibleMove.getSteps().get(possibleMove.getSteps().size() - 1).getName();
        this.cost = possibleMove.getCost();
        this.steps = possibleMove.getSteps().stream()
                .map(Location::getName)
                .collect(Collectors.toList());
        this.playerFees = possibleMove.getPlayerFees().entrySet().stream()
                .map(entry -> new PlayerFeeView(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
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
