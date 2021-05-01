package com.boardgamefiesta.server.rest.table.view;

import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class TableView {

    String id;
    String game;
    Table.Type type;
    Table.Mode mode;
    Table.Visibility visibility;
    Table.Status status;
    Instant created;
    Instant started;
    Instant ended;
    UserView owner;
    String player;
    Set<String> otherPlayers;
    Map<String, PlayerView> players;
    int numberOfPlayers;
    boolean canAccept;
    boolean canStart;
    boolean canJoin;
    boolean canLeave;

    @Deprecated
    boolean accepted;

    @Deprecated
    boolean startable;

    Map<String, Object> options;

    Boolean turn;

    /**
     * Deprecated in favor of {@link #currentPlayers}.
     */
    @Deprecated
    String currentPlayer;
    Set<String> currentPlayers;
    Boolean canUndo;

    int minNumberOfPlayers;
    int maxNumberOfPlayers;
    int minNumberOfPlayersGame;
    int maxNumberOfPlayersGame;
    boolean autoStart;

    public TableView(@NonNull Table table,
                     @NonNull Map<User.Id, User> userMap,
                     @NonNull Map<User.Id, Rating> ratingMap,
                     User.Id currentUserId) {
        id = table.getId().getId();
        game = table.getGame().getId().getId();
        type = table.getType();
        mode = table.getMode();
        visibility = table.getVisibility();
        status = table.getStatus();
        owner = new UserView(table.getOwnerId(), userMap.get(table.getOwnerId()), currentUserId);
        options = table.getOptions().asMap();

        player = table.getPlayerByUserId(currentUserId)
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

        numberOfPlayers = table.getPlayers().size();

        created = table.getCreated();
        started = table.getStarted();
        ended = table.getEnded();
        canStart = table.canStart() && currentUserId.equals(table.getOwnerId());
        canJoin = table.canJoin(currentUserId);
        canAccept = table.getPlayerByUserId(currentUserId)
                .map(player -> player.getStatus() == Player.Status.INVITED)
                .orElse(false);
        canLeave = table.canLeave(currentUserId);

        // TODO Remove when frontend is using canStart and canAccept
        startable = canStart;
        accepted = table.getPlayers().stream()
                .filter(player -> currentUserId.equals(player.getUserId().orElse(null)))
                .anyMatch(player -> player.getStatus() == Player.Status.ACCEPTED);

        minNumberOfPlayers = table.getMinNumberOfPlayers();
        maxNumberOfPlayers = table.getMaxNumberOfPlayers();
        minNumberOfPlayersGame = table.getGame().getMinNumberOfPlayers();
        maxNumberOfPlayersGame = table.getGame().getMaxNumberOfPlayers();
        autoStart = table.isAutoStart();

        if (table.getStatus() == Table.Status.STARTED) {
            var currentPlayers = table.getCurrentPlayers();

            this.turn = currentPlayers.stream().map(Player::getUserId).flatMap(Optional::stream).anyMatch(userId -> userId.equals(currentUserId));

            // For backwards compatibility
            this.currentPlayer = !currentPlayers.isEmpty() ? currentPlayers.iterator().next().getId().getId() : null;

            this.currentPlayers = table.getCurrentPlayers().stream().map(Player::getId).map(Player.Id::getId).collect(Collectors.toSet());


            this.canUndo = true;
            // TODO Replace with something more efficient, as Table#canUndo now has to load the entire state
//            this.canUndo = table.canUndo();
        } else {
            this.turn = null;
            this.currentPlayer = null;
            this.currentPlayers = null;
            this.canUndo = null;
        }
    }
}
