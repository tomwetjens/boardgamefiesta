package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Game;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.domain.ActionType;
import lombok.Value;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class StateView {

    RailroadTrackView railroadTrack;
    PlayerStateView player;
    List<PlayerStateView> otherPlayers;
    ForesightsView foresights;
    TrailView trail;
    JobMarketView jobMarket;
    CattleMarketView cattleMarket;
    ObjectiveCardsView objectiveCards;
    PlayerView currentPlayer;
    List<ActionType> actions;
    boolean turn;

    public StateView(Game state, Player viewingPlayer) {
        railroadTrack = new RailroadTrackView(state.getRailroadTrack());

        player = state.getPlayers().stream()
                .filter(p -> p == viewingPlayer)
                .map(state::playerState)
                .map(playerState -> new PlayerStateView(state, playerState, viewingPlayer))
                .findAny()
                .orElse(null);

        // Other players in play order
        var viewingPlayerIndex = state.getPlayers().indexOf(viewingPlayer);
        var playerCount = state.getPlayers().size();
        otherPlayers = IntStream.range(1, playerCount)
                .map(i -> (viewingPlayerIndex + i) % playerCount)
                .mapToObj(i -> state.getPlayers().get(i))
                .map(p -> new PlayerStateView(state, state.playerState(p), viewingPlayer))
                .collect(Collectors.toList());

        foresights = new ForesightsView(state.getForesights());

        trail = new TrailView(state.getTrail());

        jobMarket = new JobMarketView(state.getJobMarket());

        cattleMarket = new CattleMarketView(state.getCattleMarket());

        objectiveCards = new ObjectiveCardsView(state.getObjectiveCards());

        currentPlayer = new PlayerView(state.getCurrentPlayer());

        if (viewingPlayer == state.getCurrentPlayer()) {
            actions = state.possibleActions().stream()
                    .map(ActionType::of)
                    .sorted(Comparator.comparing(Enum::name))
                    .collect(Collectors.toList());
            turn = true;
        } else {
            actions = Collections.emptyList();
            turn = false;
        }
    }

}
