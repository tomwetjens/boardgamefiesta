package com.boardgamefiesta.server.rest.table.view;

import com.boardgamefiesta.server.domain.Player;
import com.boardgamefiesta.server.domain.Table;
import com.boardgamefiesta.server.domain.User;
import com.boardgamefiesta.server.domain.rating.Rating;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.NonNull;
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
    Boolean canUndo;

    public TableView(@NonNull Table table,
                     @NonNull Map<User.Id, User> userMap,
                     @NonNull Map<User.Id, Rating> ratingMap,
                     User.Id currentUserId) {
        id = table.getId().getId();
        game = table.getGame().getId().getId();
        type = table.getType();
        status = table.getStatus();
        owner = new UserView(table.getOwnerId(), userMap.get(table.getOwnerId()), currentUserId);
        options = table.getOptions().asMap();

        player = table.getPlayers().stream()
                .filter(player -> currentUserId.equals(player.getUserId().orElse(null)))
                .findAny()
                .map(Player::getId)
                .map(Player.Id::getId)
                .orElse(null);

        otherPlayers = table.getPlayers().stream()
                .filter(player -> !currentUserId.equals(player.getUserId().orElse(null)))
                .map(Player::getId)
                .map(Player.Id::getId)
                .collect(Collectors.toSet());

        players = table.getPlayers().stream()
                .collect(Collectors.toMap(player -> player.getId().getId(),
                        player -> new PlayerView(player, userMap::get, ratingMap)));

        accepted = table.getPlayers().stream()
                .filter(player -> currentUserId.equals(player.getUserId().orElse(null)))
                .anyMatch(player -> player.getStatus() == Player.Status.ACCEPTED);
        created = table.getCreated();
        started = table.getStarted();
        ended = table.getEnded();
        startable = table.canStart() && currentUserId.equals(table.getOwnerId());

        if (table.getStatus() == Table.Status.STARTED) {
            var currentPlayer = table.getCurrentPlayer();

            this.turn = currentUserId.equals(currentPlayer.getUserId().orElse(null));
            this.currentPlayer = currentPlayer.getId().getId();
            this.canUndo = table.canUndo();
        } else {
            this.turn = null;
            this.currentPlayer = null;
            this.canUndo = null;
        }
    }
}
