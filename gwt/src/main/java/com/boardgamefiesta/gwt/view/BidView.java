package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.gwt.logic.Bid;
import lombok.Value;

@Value
public class BidView {

    String player;
    Integer position;
    Integer points;

    BidView(Player player) {
        this.player = player.getName();
        this.position = null;
        this.points = null;
    }

    BidView(Player player, Bid bid) {
        this.player = player.getName();
        this.position = bid.getPosition();
        this.points = bid.getPoints();
    }
}
