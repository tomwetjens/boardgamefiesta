package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.Game;
import com.wetjens.gwt.Player;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
public class StateView {

    RailroadTrackView railroadTrack;
    PlayerStateView player;
    Map<String, PlayerStateView> otherPlayers;
    Map<String, PlayerView> players;
    List<String> playerOrder;
    ForesightsView foresights;
    TrailView trail;
    JobMarketView jobMarket;
    CattleMarketView cattleMarket;
    Set<ObjectiveCardView> objectiveCards;

    String currentPlayer;
    Set<ActionView> actions;

    public StateView(Game game, Player viewingPlayer) {
        railroadTrack = new RailroadTrackView(game.getRailroadTrack());

        player = game.getPlayers().stream()
                .filter(p -> p == viewingPlayer)
                .map(game::playerState)
                .map(playerState -> new PlayerStateView(playerState, viewingPlayer))
                .findAny()
                .orElse(null);

        otherPlayers = game.getPlayers().stream()
                .filter(p -> p != viewingPlayer)
                .collect(Collectors.toMap(Player::getName, p -> new PlayerStateView(game.playerState(p), viewingPlayer)));

        players = game.getPlayers().stream().map(PlayerView::new).collect(Collectors.toMap(PlayerView::getName, Function.identity()));
        playerOrder = game.getPlayers().stream().map(Player::getName).collect(Collectors.toList());

        foresights = new ForesightsView(game.getForesights());

        trail = new TrailView(game.getTrail());

        jobMarket = new JobMarketView(game.getJobMarket());

        cattleMarket = new CattleMarketView(game.getCattleMarket());

        objectiveCards = game.getObjectiveCards().getAvailable().stream().map(ObjectiveCardView::new).collect(Collectors.toSet());

        currentPlayer = game.getCurrentPlayer().getName();

        if (viewingPlayer == game.getCurrentPlayer()) {
            actions = game.possibleActions().stream().map(ActionView::of).collect(Collectors.toSet());
        } else {
            actions = Collections.emptySet();
        }
    }

}
