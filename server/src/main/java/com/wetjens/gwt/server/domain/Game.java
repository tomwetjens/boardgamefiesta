package com.wetjens.gwt.server.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(doNotUseGetters = true)
public class Game {

    private static final Duration START_TIMEOUT = Duration.of(2, ChronoUnit.DAYS);
    private static final Duration ACTION_TIMEOUT = Duration.of(1, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ENDED = Duration.of(2, ChronoUnit.DAYS);

    @Getter
    @NonNull
    private final Id id;

    @Getter
    @NonNull
    private User.Id owner;

    @NonNull
    private final Set<Player> players;

    @Getter
    @NonNull
    private Status status;

    @Getter
    private com.wetjens.gwt.Game state;

    @Getter
    @NonNull
    private Instant expires;

    @Getter
    @NonNull
    private Instant created;

    @Getter
    private Instant updated;

    @Getter
    private Instant started;

    @Getter
    private Instant ended;

    public static Game create(User owner, Set<User> inviteUsers) {
        Instant created = Instant.now();

        return Game.builder()
                .id(Id.generate())
                .status(Status.NEW)
                .created(created)
                .updated(created)
                .expires(created.plus(START_TIMEOUT))
                .owner(owner.getId())
                .players(Stream.concat(
                        Stream.of(Player.createAccepted(owner.getId())),
                        inviteUsers.stream().map(user -> Player.invite(user.getId())))
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
                        .beginner(true)
                        .build(), new Random());

        status = Status.STARTED;
        started = Instant.now();
        updated = started;
        expires = started.plus(ACTION_TIMEOUT);
    }

    public void perform(Action action) {
        state.perform(action, new Random());
        afterAction();
    }

    public void endTurn() {
        state.endTurn(new Random());
        afterAction();
    }

    private void afterAction() {
        updated = Instant.now();

        if (state.isEnded()) {
            status = Status.ENDED;
            ended = updated;
            expires = ended.plus(RETENTION_AFTER_ENDED);

            // TODO Maybe store scores on this aggregate
        } else {
            expires = updated.plus(ACTION_TIMEOUT);
        }
    }

    public void acceptInvite(User.Id userId) {
        Player player = players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Not invited"));

        player.accept();

        updated = Instant.now();
    }

    public void rejectInvite(User.Id userId) {
        Player player = players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Not invited"));

        player.reject();

        updated = Instant.now();
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
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
