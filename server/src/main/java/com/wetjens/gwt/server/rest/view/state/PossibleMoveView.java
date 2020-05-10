package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PossibleMove;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
// consider routes with exactly the same costs and fees to be equal, ignoring the actual steps
@EqualsAndHashCode(of = {"from", "to", "cost", "playerFees"})
public class PossibleMoveView {

    String from;
    String to;
    int cost;
    List<String> steps;
    List<PlayerFeeView> playerFees;

    public PossibleMoveView(PossibleMove possibleMove) {
        this.from = possibleMove.getFrom().getName();
        this.to = possibleMove.getTo().getName();
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
