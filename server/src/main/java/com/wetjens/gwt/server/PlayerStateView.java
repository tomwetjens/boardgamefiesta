package com.wetjens.gwt.server;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerState;
import lombok.Value;

import java.util.Set;
import java.util.stream.Collectors;

@Value
public class PlayerStateView {

    PlayerView player;

    int balance;
    int cowboys;
    int craftsmen;
    int engineers;

    Set<CardView> hand;

    Set<ActionView> unlockedSingleAuxiliaryActions;
    Set<ActionView> unlockedDoubleAuxiliaryActions;

    PlayerStateView(PlayerState playerState, Player viewingPlayer) {
        player = new PlayerView(playerState.getPlayer());

        balance = playerState.getBalance();
        cowboys = playerState.getNumberOfCowboys();
        craftsmen = playerState.getNumberOfCraftsmen();
        engineers = playerState.getNumberOfEngineers();

        if (viewingPlayer == playerState.getPlayer()) {
            hand = playerState.getHand().stream()
                    .map(CardView::of)
                    .collect(Collectors.toSet());
        } else {
            hand = null;
        }

        unlockedSingleAuxiliaryActions = playerState.unlockedSingleAuxiliaryActions().stream()
                .map(ActionView::of)
                .collect(Collectors.toSet());

        unlockedDoubleAuxiliaryActions = playerState.unlockedDoubleAuxiliaryActions().stream()
                .map(ActionView::of)
                .collect(Collectors.toSet());
    }

}
