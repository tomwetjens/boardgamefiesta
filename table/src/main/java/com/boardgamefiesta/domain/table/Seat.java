package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.domain.user.User;
import lombok.*;

import java.util.Optional;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@ToString
public final class Seat {

    @Getter
    @NonNull
    Player.Id playerId;

    PlayerColor playerColor;

    Player player;

    public static Seat empty() {
        return new Seat(Player.Id.generate(), null, null);
    }

    public Optional<PlayerColor> getPlayerColor() {
        return Optional.ofNullable(playerColor);
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    public static Seat fork(Player player) {
        return new Seat(player.getId(), player.getColor().orElse(null), null);
    }

    public static Seat fromPlayer(Player player) {
        return new Seat(player.getId(), player.getColor().orElse(null), player);
    }

    Player invite(User user) {
        player = Player.invite(playerId, user.getId());
        if (playerColor != null) {
            player.assignColor(playerColor);
        }
        return player;
    }

    Player assign(User user, Set<PlayerColor> availableColors) {
        return assign(Player.accepted(playerId, user, availableColors));
    }

    Player assign(Player player) {
        this.player = Player.copy(playerId, player);

        if (playerColor != null) {
            this.player.assignColor(playerColor);
        }

        return this.player;
    }

    Player computer() {
        return assign(Player.computer(playerId));
    }

    Optional<Player> unassign() {
        var player = this.player;
        this.player = null;
        return Optional.ofNullable(player);
    }

    public boolean isAvailable() {
        return player == null;
    }

}
