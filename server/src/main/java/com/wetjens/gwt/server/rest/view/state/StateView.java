package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;
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
    Map<Player, PlayerStateView> otherPlayers;
    Map<Player, PlayerView> players;
    List<Player> playerOrder;
    ForesightsView foresights;
    TrailView trail;
    JobMarketView jobMarket;
    CattleMarketView cattleMarket;
    Set<ObjectiveCardView> objectiveCards;

    PlayerView currentPlayer;
    Set<ActionType> actions;
    Instant expires;
    boolean turn;

    public StateView(Game game, Player viewingPlayer, Map<Player, User> userMap) {
        railroadTrack = new RailroadTrackView(game.getState().getRailroadTrack());

        player = game.getState().getPlayers().stream()
                .filter(p -> p == viewingPlayer)
                .map(game.getState()::playerState)
                .map(playerState -> new PlayerStateView(playerState, viewingPlayer, userMap.get(playerState.getPlayer())))
                .findAny()
                .orElse(null);

        otherPlayers = game.getState().getPlayers().stream()
                .filter(p -> p != viewingPlayer)
                .collect(Collectors.toMap(Function.identity(), p -> new PlayerStateView(game.getState().playerState(p), viewingPlayer, userMap.get(p))));

        players = game.getState().getPlayers().stream()
                .map(player -> new PlayerView(player, userMap.get(player)))
                .collect(Collectors.toMap(PlayerView::getColor, Function.identity()));
        playerOrder = game.getState().getPlayers();

        foresights = new ForesightsView(game.getState().getForesights());

        trail = new TrailView(game.getState().getTrail());

        jobMarket = new JobMarketView(game.getState().getJobMarket());

        cattleMarket = new CattleMarketView(game.getState().getCattleMarket());

        objectiveCards = game.getState().getObjectiveCards().getAvailable().stream().map(ObjectiveCardView::new).collect(Collectors.toSet());

        currentPlayer = new PlayerView(game.getState().getCurrentPlayer(), userMap.get(game.getState().getCurrentPlayer()));

        if (viewingPlayer == game.getState().getCurrentPlayer()) {
            actions = game.getState().possibleActions().stream().map(ActionType::of).collect(Collectors.toSet());
            turn = true;
        } else {
            actions = Collections.emptySet();
            turn = false;
        }

        expires = game.getExpires();
    }

}
