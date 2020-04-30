package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PossibleMove;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.rest.view.UserView;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(of={"from", "to", "cost", "playerFees"}) // consider routes with exactly the same costs and fees to be equal, ignoring the actual steps
public class PossibleMoveView {

    String from;
    String to;
    int cost;
    List<String> steps;
    List<PlayerFeeView> playerFees;

    public PossibleMoveView(PossibleMove possibleMove, Map<Player, User> userMap) {
        this.from = possibleMove.getFrom().getName();
        this.to = possibleMove.getTo().getName();
        this.cost = possibleMove.getCost();
        this.steps = possibleMove.getSteps().stream()
                .map(Location::getName)
                .collect(Collectors.toList());
        this.playerFees = possibleMove.getPlayerFees().entrySet().stream()
                .map(entry -> new PlayerFeeView(new PlayerView(entry.getKey(), userMap.get(entry.getKey())), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Value
    public static class PlayerFeeView {
        PlayerView player;
        int fee;
    }
}
