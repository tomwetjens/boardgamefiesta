package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.Action;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.ForbiddenException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private boolean beginner;

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

    public static Game create(@NonNull User owner, @NonNull Set<User> inviteUsers, boolean beginner) {
        Games games = CDI.current().select(Games.class).get();

        int count = games.countByUserId(owner.getId());
        if (count >= 3) {
            throw new IllegalStateException("Cannot participate in more than 3 games");
        }

        if (inviteUsers.contains(owner)) {
            throw new IllegalArgumentException("Cannot invite yourself");
        }

        if (inviteUsers.isEmpty()) {
            throw new IllegalArgumentException("Should invite at least 1 user");
        }

        if (inviteUsers.size() > 5) {
            throw new IllegalArgumentException("Should invite at most 5 users");
        }

        var created = Instant.now();

        Game game = Game.builder()
                .id(Id.generate())
                .status(Status.NEW)
                .beginner(beginner)
                .created(created)
                .updated(created)
                .expires(created.plus(START_TIMEOUT))
                .owner(owner.getId())
                .players(Stream.concat(
                        Stream.of(Player.createAccepted(owner.getId())),
                        inviteUsers.stream().map(user -> Player.invite(user.getId())))
                        .collect(Collectors.toSet()))
                .build();

        for (User user : inviteUsers) {
            CDI.current().getBeanManager().fireEvent(game.new Invited(user.getId()));
        }

        return game;
    }

    public void start() {
        if (status != Status.NEW) {
            throw new IllegalStateException("Already started or ended");
        }

        players.removeIf(player -> player.getStatus() != Player.Status.ACCEPTED);

        Random random = new Random();

        assignColors(random);

        state = new com.wetjens.gwt.Game(players.stream().map(Player::getColor).collect(Collectors.toSet()), beginner, random);

        status = Status.STARTED;
        started = Instant.now();
        updated = started;
        expires = started.plus(ACTION_TIMEOUT);

        CDI.current().getBeanManager().fireEvent(new Started());
    }

    private void assignColors(Random random) {
        var randomColors = new LinkedList<>(Arrays.asList(com.wetjens.gwt.Player.values()));
        Collections.shuffle(randomColors, random);

        players.forEach(player -> player.setColor(randomColors.poll()));
    }

    public void perform(Action action) {
        if (status != Status.STARTED) {
            throw new IllegalStateException("Not started");
        }

        state.perform(action, new Random());
        afterAction();
    }

    public void endTurn() {
        if (status != Status.STARTED) {
            throw new IllegalStateException("Not started");
        }

        state.endTurn(new Random());
        afterAction();
    }

    private void afterAction() {
        updated = Instant.now();

        CDI.current().getBeanManager().fireEvent(new StateChanged());

        if (state.isEnded()) {
            status = Status.ENDED;
            ended = updated;
            expires = ended.plus(RETENTION_AFTER_ENDED);

            Set<com.wetjens.gwt.Player> winners = state.winners();
            players.forEach(player -> {
                player.setScore(state.score(player.getColor()));
                player.setWinner(winners.contains(player.getColor()));
            });

            CDI.current().getBeanManager().fireEvent(new Ended());
        } else {
            expires = updated.plus(ACTION_TIMEOUT);
        }
    }

    public void acceptInvite(User.Id userId) {
        if (status != Status.NEW) {
            throw new IllegalStateException("Already started or ended");
        }

        var player = players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Not invited"));

        player.accept();

        updated = Instant.now();

        CDI.current().getBeanManager().fireEvent(new Accepted(userId));

        if (allPlayersResponded()) {
            if (numberOfPlayersAccepted() >= 2) {
                // If enough players have accepted, automatically start
                start();
            } else {
                abandon();
            }
        }
    }

    public void abandon() {
        if (status != Status.NEW) {
            throw new IllegalStateException("Already started or ended");
        }

        expires = Instant.now();
    }

    private int numberOfPlayersAccepted() {
        return (int) players.stream().filter(p -> p.getStatus() == Player.Status.ACCEPTED).count();
    }

    private boolean allPlayersResponded() {
        return players.stream().allMatch(p -> p.getStatus() == Player.Status.ACCEPTED || p.getStatus() == Player.Status.REJECTED);
    }

    public void rejectInvite(User.Id userId) {
        if (status != Status.NEW) {
            throw new IllegalStateException("Already started or ended");
        }

        var player = players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Not invited"));

        player.reject();

        updated = Instant.now();

        CDI.current().getBeanManager().fireEvent(new Rejected(userId));
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean canStart() {
        return status == Status.NEW && players.stream().filter(p -> p.getStatus() == Player.Status.ACCEPTED).count() > 1;
    }

    public Optional<Player> getPlayerByUserId(User.Id userId) {
        return players.stream()
                .filter(player -> player.getUserId().equals(userId))
                .findAny();
    }

    public Player getCurrentPlayer() {
        if (state == null) {
            return null;
        }

        return players.stream()
                .filter(player -> player.getColor() == state.getCurrentPlayer())
                .findAny()
                .orElse(null);
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

    @Value
    public final class Invited {
        Game game = Game.this;
        User.Id userId;
    }

    @Value
    public final class Accepted {
        Game game = Game.this;
        User.Id userId;
    }

    @Value
    public final class Rejected {
        Game game = Game.this;
        User.Id userId;
    }

    @Value
    public final class Started {
        Game game = Game.this;
    }

    @Value
    public final class Ended {
        Game game = Game.this;
    }

    @Value
    public final class StateChanged {
        Game game = Game.this;
    }
}
