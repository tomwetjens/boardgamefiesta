package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class GameView {

    String id;
    Game.Status status;
    Instant created;
    Instant started;
    Instant ended;
    UserView owner;
    String player;
    Set<String> otherPlayers;
    Map<String, PlayerView> players;
    boolean accepted;
    boolean startable;

    Boolean turn;
    String currentPlayer;

    public GameView(Game game, Map<User.Id, User> userMap, User.Id currentUserId) {
        id = game.getId().getId();
        status = game.getStatus();
        owner = new UserView(game.getOwner(), userMap.get(game.getOwner()));

        player = game.getPlayers().stream()
                .filter(player -> currentUserId.equals(player.getUserId()))
                .findAny()
                .map(Player::getId)
                .map(Player.Id::getId)
                .orElse(null);

        otherPlayers = game.getPlayers().stream()
                .filter(player -> !currentUserId.equals(player.getUserId()))
                .map(Player::getId)
                .map(Player.Id::getId)
                .collect(Collectors.toSet());

        players = game.getPlayers().stream()
                .collect(Collectors.toMap(player -> player.getId().getId(), player -> new PlayerView(player, userMap.get(player.getUserId()))));

        accepted = game.getPlayers().stream()
                .filter(player -> currentUserId.equals(player.getUserId()))
                .anyMatch(player -> player.getStatus() == Player.Status.ACCEPTED);
        created = game.getCreated();
        started = game.getStarted();
        ended = game.getEnded();
        startable = game.canStart() && currentUserId.equals(game.getOwner());

        if (game.getStatus() == Game.Status.STARTED) {
            var currentPlayer = game.getCurrentPlayer();

            this.turn = currentUserId.equals(currentPlayer.getUserId());
            this.currentPlayer = currentPlayer.getId().getId();
        } else {
            this.turn = null;
            this.currentPlayer = null;
        }
    }
}
