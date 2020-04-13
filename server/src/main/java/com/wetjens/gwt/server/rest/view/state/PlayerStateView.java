package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class PlayerStateView {

    PlayerView player;

    int balance;
    int cowboys;
    int craftsmen;
    int engineers;
    int certificates;

    Set<CardView> hand;
    List<CardView> discardPile;
    Integer drawStackSize;

    Set<ActionType> unlockedSingleAuxiliaryActions;
    Set<ActionType> unlockedDoubleAuxiliaryActions;

    PlayerStateView(PlayerState playerState, Player viewingPlayer, User user) {
        player = new PlayerView(playerState.getPlayer(), user);

        balance = playerState.getBalance();
        cowboys = playerState.getNumberOfCowboys();
        craftsmen = playerState.getNumberOfCraftsmen();
        engineers = playerState.getNumberOfEngineers();
        certificates = playerState.getCertificates();

        if (viewingPlayer == playerState.getPlayer()) {
            hand = playerState.getHand().stream()
                    .map(CardView::of)
                    .collect(Collectors.toSet());
            discardPile = playerState.getDiscardPile().stream()
                    .map(CardView::of)
                    .collect(Collectors.toList());
            drawStackSize = playerState.getDrawStackSize();
        } else {
            hand = null;
            discardPile = null;
            drawStackSize = null;
        }

        unlockedSingleAuxiliaryActions = playerState.unlockedSingleAuxiliaryActions().stream()
                .map(ActionType::of)
                .collect(Collectors.toSet());

        unlockedDoubleAuxiliaryActions = playerState.unlockedDoubleAuxiliaryActions().stream()
                .map(ActionType::of)
                .collect(Collectors.toSet());
    }

}
