package com.wetjens.gwt.server.domain;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wetjens.gwt.Action;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(doNotUseGetters = true)
public class Game {

    @Getter
    private final Id id;

    @Getter
    private User.Id owner;
    private final Set<Player> players;

    @Getter
    private Status status;

    @Getter
    private com.wetjens.gwt.Game state;

    public static Game create(User owner, Set<User> inviteUsers) {
        return Game.builder()
                .id(Id.generate())
                .status(Status.NEW)
                .owner(owner.getId())
                .players(Stream.concat(
                        Stream.of(Player.builder().userId(owner.getId()).status(Player.Status.ACCEPTED).build()),
                        inviteUsers.stream()
                                .map(user -> Player.builder().userId(user.getId()).status(Player.Status.INVITED).build()))
                        .collect(Collectors.toSet()))
                .build();
    }

    public void start() {
        state = new com.wetjens.gwt.Game(players.stream()
                .filter(player -> player.getStatus() == Player.Status.ACCEPTED)
                .map(Player::getUserId)
                .map(User.Id::getId)
                .collect(Collectors.toSet()),
                // TODO Get options from command
                com.wetjens.gwt.Game.Options.builder()
                        .beginner(false)
                        .build(), new Random());

        status = Status.STARTED;
    }

    public void perform(Action action) {
        state.perform(action);

        if (state.isEnded()) {
            status = Status.ENDED;
        }

        // TODO Maybe store scores on this aggregate
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public void acceptInvite(User.Id userId) {
        Player player = players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .filter(p -> p.getStatus() == Player.Status.INVITED)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Not invited"));

        player.accept();
    }

    public void rejectInvite(User.Id userId) {
        Player player = players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .filter(p -> p.getStatus() == Player.Status.INVITED)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Not invited"));

        player.reject();
    }

    public enum Status {
        NEW,
        STARTED,
        ENDED;
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;

        private static Id generate() {
            return of(UUID.randomUUID().toString());
        }
    }
}
