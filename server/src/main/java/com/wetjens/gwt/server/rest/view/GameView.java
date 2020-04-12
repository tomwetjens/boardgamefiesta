package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class GameView {

    String id;
    Game.Status status;
    Instant created;
    Instant started;
    Instant ended;
    Instant expires;
    UserView owner;
    Set<PlayerView> otherPlayers;
    boolean accepted;
    boolean startable;

    Boolean turn;
    UserView currentPlayer;

    public GameView(Game game, Map<User.Id, User> userMap, User.Id currentUserId) {
        id = game.getId().getId();
        status = game.getStatus();
        owner = new UserView(game.getOwner(), userMap.get(game.getOwner()));
        otherPlayers = game.getPlayers().stream()
                .filter(player -> !player.getUserId().equals(currentUserId))
                .map(player -> new PlayerView(player, userMap.get(player.getUserId())))
                .collect(Collectors.toSet());
        accepted = game.getPlayers().stream()
                .filter(player -> player.getUserId().equals(currentUserId))
                .anyMatch(player -> player.getStatus() == Player.Status.ACCEPTED);
        created = game.getCreated();
        started = game.getStarted();
        ended = game.getEnded();
        expires = game.getExpires();
        startable = game.canStart() && currentUserId.equals(game.getOwner());

        if(game.getState() != null) {
            Player currentUserPlayer = game.getPlayerByUserId(currentUserId).orElseThrow(() -> new IllegalArgumentException("Not a player in game"));

            turn =  game.getState().getCurrentPlayer() == currentUserPlayer.getColor();

            Player currentPlayer = game.getCurrentPlayer();
            this.currentPlayer = new UserView(currentPlayer.getUserId(), userMap.get(currentPlayer.getUserId()));
        } else {
            turn = null;
            currentPlayer = null;
        }
    }
}
