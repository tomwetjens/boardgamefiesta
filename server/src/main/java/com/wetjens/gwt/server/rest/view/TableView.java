package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.Table;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class TableView {

    String id;
    String game;
    Table.Type type;
    Table.Status status;
    Instant created;
    Instant started;
    Instant ended;
    UserView owner;
    String player;
    Set<String> otherPlayers;
    Map<String, PlayerView> players;
    boolean accepted;
    boolean startable;
    Map<String, Object> options;

    Boolean turn;
    String currentPlayer;

    public TableView(Table table, Map<User.Id, User> userMap, User.Id currentUserId) {
        id = table.getId().getId();
        game = table.getGame().getId();
        type = table.getType();
        status = table.getStatus();
        owner = new UserView(table.getOwner(), userMap.get(table.getOwner()), currentUserId);
        options = table.getOptions().asMap();

        player = table.getPlayers().stream()
                .filter(player -> currentUserId.equals(player.getUserId()))
                .findAny()
                .map(Player::getId)
                .map(Player.Id::getId)
                .orElse(null);

        otherPlayers = table.getPlayers().stream()
                .filter(player -> !currentUserId.equals(player.getUserId()))
                .map(Player::getId)
                .map(Player.Id::getId)
                .collect(Collectors.toSet());

        players = table.getPlayers().stream()
                .collect(Collectors.toMap(player -> player.getId().getId(), player -> new PlayerView(player, userMap.get(player.getUserId()))));

        accepted = table.getPlayers().stream()
                .filter(player -> currentUserId.equals(player.getUserId()))
                .anyMatch(player -> player.getStatus() == Player.Status.ACCEPTED);
        created = table.getCreated();
        started = table.getStarted();
        ended = table.getEnded();
        startable = table.canStart() && currentUserId.equals(table.getOwner());

        if (table.getStatus() == Table.Status.STARTED) {
            var currentPlayer = table.getCurrentPlayer();

            this.turn = currentUserId.equals(currentPlayer.getUserId());
            this.currentPlayer = currentPlayer.getId().getId();
        } else {
            this.turn = null;
            this.currentPlayer = null;
        }
    }
}
