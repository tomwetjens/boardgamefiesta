package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.api.Player;
import com.boardgamefiesta.api.PlayerColor;
import com.boardgamefiesta.istanbul.logic.Game;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class IstanbulView {

    private final List<List<PlaceView>> layout;
    private final List<ActionView> actions;
    private final Map<PlayerColor, PlayerStateView> players;

    public IstanbulView(Game state, Player viewer) {
        this.layout = IntStream.range(0, state.getLayout().width())
                .mapToObj(x -> IntStream.range(0, state.getLayout().height())
                        .mapToObj(y -> state.getLayout().place(x, y))
                        .map(PlaceView::of)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        this.actions = state.getCurrentPlayer() == viewer
                ? state.getPossibleActions().stream()
                .map(ActionView::of)
                .sorted()
                .collect(Collectors.toList())
                : null;

        this.players = state.getPlayers().stream().collect(Collectors.toMap(Player::getColor, player ->
                new PlayerStateView(state.getPlayerState(player), player == viewer)));
    }

}
