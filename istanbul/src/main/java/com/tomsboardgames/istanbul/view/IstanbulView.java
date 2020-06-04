package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import com.tomsboardgames.istanbul.logic.Game;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class IstanbulView {

    private final List<List<PlaceView>> layout;
    private final Set<ActionView> actions;
    private final Map<PlayerColor, PlayerStateView> players;

    public IstanbulView(Game state, Player viewer) {
        this.layout = Arrays.stream(state.getLayout())
                .map(places -> Arrays.stream(places)
                        .map(PlaceView::of)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        this.actions = state.getCurrentPlayer() == viewer
                ? state.getPossibleActions().map(ActionView::of).collect(Collectors.toSet())
                : null;

        this.players = state.getPlayers().stream().collect(Collectors.toMap(Player::getColor, player ->
                new PlayerStateView(state.getPlayerState(player), player == viewer)));
    }

}
