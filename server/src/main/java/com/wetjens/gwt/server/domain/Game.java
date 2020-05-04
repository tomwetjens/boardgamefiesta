package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Automa;
import com.wetjens.gwt.server.rest.APIError;
import com.wetjens.gwt.server.rest.APIException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(doNotUseGetters = true)
@Slf4j
public class Game {

    private static final Duration START_TIMEOUT = Duration.of(2, ChronoUnit.DAYS);
    private static final Duration ACTION_TIMEOUT = Duration.of(1, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ENDED = Duration.of(2, ChronoUnit.DAYS);

    private static final Random RANDOM = new Random();

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

    public static Game create(@NonNull User owner, int numberOfPlayers, @NonNull Set<User> inviteUsers, boolean beginner) {
        int minNumberOfPlayers = 2;
        int maxNumberOfPlayers = 4;

        if (numberOfPlayers < minNumberOfPlayers) {
            throw APIException.badRequest(APIError.MIN_PLAYERS);
        }

        if (numberOfPlayers > maxNumberOfPlayers) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        if (inviteUsers.contains(owner)) {
            throw APIException.badRequest(APIError.CANNOT_INVITE_YOURSELF);
        }

        if (inviteUsers.size() > numberOfPlayers - 1) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        Games games = CDI.current().select(Games.class).get();
        if (games.countByUserId(owner.getId()) >= 3) {
            throw APIException.forbidden(APIError.REACHED_MAX_GAMES);
        }

        var player = Player.accepted(owner.getId());
        var invitedPlayers = inviteUsers.stream().map(user -> Player.invite(user.getId()));
        var computerPlayers = IntStream.range(0, numberOfPlayers - inviteUsers.size() - 1).mapToObj(i -> Player.computer());

        var created = Instant.now();
        Game game = Game.builder()
                .id(Id.generate())
                .status(Status.NEW)
                .beginner(beginner)
                .created(created)
                .updated(created)
                .expires(created.plus(START_TIMEOUT))
                .owner(owner.getId())
                .players(Stream.concat(Stream.concat(Stream.of(player), invitedPlayers), computerPlayers)
                        .collect(Collectors.toSet()))
                .build();

        CDI.current().getBeanManager().fireEvent(game.new Created());

        for (User user : inviteUsers) {
            CDI.current().getBeanManager().fireEvent(game.new Invited(user.getId()));
        }

        game.afterRespond();

        return game;
    }

    public void start() {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        players.removeIf(player -> player.getStatus() != Player.Status.ACCEPTED);

        assignColors();

        // TODO Pass player ids
        state = new com.wetjens.gwt.Game(players.stream().map(Player::getColor).collect(Collectors.toSet()), beginner, RANDOM);
        afterStateChange();

        status = Status.STARTED;
        started = Instant.now();
        updated = started;
        expires = started.plus(ACTION_TIMEOUT);
    }

    private void assignColors() {
        var randomColors = new LinkedList<>(Arrays.asList(com.wetjens.gwt.Player.values()));
        Collections.shuffle(randomColors, RANDOM);

        players.forEach(player -> player.setColor(randomColors.poll()));
    }

    public void perform(Action action) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        runStateChange(() -> state.perform(action, RANDOM));
    }

    public void executeAutoma() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        var currentPlayer = getCurrentPlayer();
        if (currentPlayer.getType() != Player.Type.COMPUTER) {
            throw new IllegalStateException("Current player is not computer");
        }

        runStateChange(() -> {
            new Automa().execute(state, RANDOM);
        });
    }

    private void runStateChange(Runnable runnable) {
        List<LogEntry> logEntries = new LinkedList<>();

        state.addEventListener(event ->
                logEntries.add(new LogEntry(this, event)));

        runnable.run();

        if (!logEntries.isEmpty()) {
            try {
                CDI.current().select(LogEntries.class).get().addAll(logEntries);
            } catch (RuntimeException e) {
                log.error("Error saving log entries", e);
            }
        }

        afterStateChange();
    }

    public void skip() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        runStateChange(() -> state.skip(RANDOM));
    }

    public void endTurn() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        runStateChange(() -> state.endTurn(RANDOM));
    }

    private void afterStateChange() {
        updated = Instant.now();

        CDI.current().getBeanManager().fireEvent(new StateChanged());

        if (state.isEnded()) {
            status = Status.ENDED;
            ended = updated;
            expires = ended.plus(RETENTION_AFTER_ENDED);

            Set<com.wetjens.gwt.Player> winners = state.winners();
            players.forEach(player -> {
                player.setScore(new Score(state.score(player.getColor()).getCategories().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue))));
                player.setWinner(winners.contains(player.getColor()));
            });

            CDI.current().getBeanManager().fireEvent(new Ended());
        } else {
            expires = updated.plus(ACTION_TIMEOUT);
        }
    }

    public void acceptInvite(@NonNull User.Id userId) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        var player = players.stream()
                .filter(p -> userId.equals(p.getUserId()))
                .findAny()
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_INVITED));

        player.accept();

        updated = Instant.now();

        CDI.current().getBeanManager().fireEvent(new Accepted(userId));

        afterRespond();
    }

    private void afterRespond() {
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
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        expires = Instant.now();
    }

    private int numberOfPlayersAccepted() {
        return (int) players.stream().filter(p -> p.getStatus() == Player.Status.ACCEPTED).count();
    }

    private boolean allPlayersResponded() {
        return players.stream().allMatch(p -> p.getStatus() == Player.Status.ACCEPTED || p.getStatus() == Player.Status.REJECTED);
    }

    public void rejectInvite(@NonNull User.Id userId) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        var player = players.stream()
                .filter(p -> userId.equals(p.getUserId()))
                .findAny()
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_INVITED));

        player.reject();

        updated = Instant.now();

        CDI.current().getBeanManager().fireEvent(new Rejected(userId));

        afterRespond();
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean canStart() {
        return status == Status.NEW && players.stream().filter(p -> p.getStatus() == Player.Status.ACCEPTED).count() > 1;
    }

    public Optional<Player> getPlayerByUserId(User.Id userId) {
        return players.stream()
                .filter(player -> userId.equals(player.getUserId()))
                .findAny();
    }

    public Player getCurrentPlayer() {
        if (state == null) {
            throw APIException.serverError(APIError.GAME_NOT_STARTED_YET);
        }

        return players.stream()
                .filter(player -> player.getColor() == state.getCurrentPlayer())
                .findAny()
                .orElseThrow(() -> APIException.serverError(APIError.NOT_PLAYER_IN_GAME));
    }

    public Optional<Player> getPlayerByColor(com.wetjens.gwt.Player color) {
        return players.stream()
                .filter(player -> player.getColor() == color)
                .findAny();
    }

    public Map<Player.Id, Player> getPlayersAsMap() {
        return players.stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
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

    @Value
    private final class Created {
        Game game = Game.this;
    }
}
