package com.wetjens.gwt.server;

import com.wetjens.gwt.Game;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerState;
import lombok.Value;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class GameView {

    RailroadTrackView railroadTrack;
    PlayerView player;
    Map<Player, PlayerView> otherPlayers;
    ForesightsView foresights;
    TrailView trail;
    JobMarketView jobMarket;
    CattleMarketView cattleMarket;
    Set<ObjectiveCardView> objectiveCards;

    Player currentPlayer;
    Set<ActionView> actions;

    GameView(Game game, Player viewingPlayer) {
        railroadTrack = new RailroadTrackView(game.getRailroadTrack());

        player = game.getPlayers().stream()
                .filter(player -> player == viewingPlayer)
                .map(game::playerState)
                .map(playerState -> new PlayerView(playerState, viewingPlayer))
                .findAny()
                .orElse(null);

        otherPlayers = game.getPlayers().stream()
                .filter(player -> player != viewingPlayer)
                .map(game::playerState)
                .collect(Collectors.toMap(PlayerState::getPlayer, playerState -> new PlayerView(playerState, viewingPlayer)));

        foresights = new ForesightsView(game.getForesights());

        trail = new TrailView(game.getTrail());

        jobMarket = new JobMarketView(game.getJobMarket());

        cattleMarket = new CattleMarketView(game.getCattleMarket());

        objectiveCards = game.getObjectiveCards().stream().map(ObjectiveCardView::new).collect(Collectors.toSet());

        currentPlayer = game.getCurrentPlayer();

        if (viewingPlayer == game.getCurrentPlayer()) {
            actions = game.possibleActions().stream().map(ActionView::of).collect(Collectors.toSet());
        } else {
            actions = Collections.emptySet();
        }
    }

}
