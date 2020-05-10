package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Automa;
import com.wetjens.gwt.GWTEventListener;
import com.wetjens.gwt.GWTException;
import com.wetjens.gwt.PlayerColor;
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
    private final Instant created;

    @NonNull
    private final Set<Player> players;

    @Getter
    private final Log log;

    @Getter
    private final boolean beginner;

    @Getter
    @NonNull
    private Status status;

    @Getter
    @NonNull
    private User.Id owner;

    @Getter
    private Lazy<com.wetjens.gwt.Game> state;

    @Getter
    @NonNull
    private Instant expires;

    @Getter
    private Instant updated;

    @Getter
    private Instant started;

    @Getter
    private Instant ended;

    public static Game create(@NonNull User creator, int numberOfPlayers, @NonNull Set<User> inviteUsers, boolean beginner) {
        int minNumberOfPlayers = 2;
        int maxNumberOfPlayers = 4;

        if (numberOfPlayers < minNumberOfPlayers) {
            throw APIException.badRequest(APIError.MIN_PLAYERS);
        }

        if (numberOfPlayers > maxNumberOfPlayers) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        if (inviteUsers.contains(creator)) {
            throw APIException.badRequest(APIError.CANNOT_INVITE_YOURSELF);
        }

        if (inviteUsers.size() > numberOfPlayers - 1) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        Games games = Games.instance();
        if (games.countByUserId(creator.getId()) >= 1) {
            throw APIException.forbidden(APIError.EXCEEDS_MAX_REALTIME_GAMES);
        }

        var player = Player.accepted(creator.getId());
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
                .owner(creator.getId())
                .players(Stream.concat(Stream.concat(Stream.of(player), invitedPlayers), computerPlayers)
                        .collect(Collectors.toSet()))
                .log(new Log())
                .build();

        game.log.add(new LogEntry(game, creator.getId(), LogEntry.Type.CREATE, Collections.emptyList()));
        inviteUsers.forEach(invitedUser -> game.log.add(new LogEntry(game, creator.getId(), LogEntry.Type.INVITE, List.of(invitedUser.getUsername()))));

        game.new Created().fire();

        for (User user : inviteUsers) {
            game.new Invited(user.getId()).fire();
        }

        game.afterRespondToInvitation();

        return game;
    }

    public void start() {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        players.removeIf(player -> player.getStatus() != Player.Status.ACCEPTED);

        var randomColors = new LinkedList<>(Arrays.asList(PlayerColor.values()));
        Collections.shuffle(randomColors, RANDOM);

        state = Lazy.of(new com.wetjens.gwt.Game(players.stream()
                .map(Player::getId)
                .map(Player.Id::getId)
                .map(name -> new com.wetjens.gwt.Player(name, randomColors.poll()))
                .collect(Collectors.toSet()), beginner, RANDOM));

        afterStateChange();

        status = Status.STARTED;
        started = Instant.now();
        updated = started;
        expires = started.plus(ACTION_TIMEOUT);

        log.add(new LogEntry(this, owner, LogEntry.Type.START, Collections.emptyList()));

        new Started().fire();
    }

    public void perform(Action action) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        try {
            runStateChange(() -> state.get().perform(action, RANDOM));
        } catch (GWTException e) {
            throw new APIException(e.getError(), e.getParams());
        }
    }

    public void executeAutoma() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        var currentPlayer = getCurrentPlayer();
        if (currentPlayer.getType() != Player.Type.COMPUTER) {
            throw new IllegalStateException("Current player is not computer");
        }

        runStateChange(() -> new Automa().execute(state.get(), RANDOM));
    }

    private void runStateChange(Runnable runnable) {
        GWTEventListener eventListener = event -> log.add(new LogEntry(this, event));
        state.get().addEventListener(eventListener);

        runnable.run();

        state.get().removeEventListener(eventListener);

        afterStateChange();
    }

    public void skip() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        try {
            runStateChange(() -> state.get().skip(RANDOM));
        } catch (GWTException e) {
            throw new APIException(e.getError(), e.getParams());
        }
    }

    public void endTurn() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }

        try {
            runStateChange(() -> state.get().endTurn(RANDOM));
        } catch (GWTException e) {
            throw new APIException(e.getError(), e.getParams());
        }
    }

    private void afterStateChange() {
        updated = Instant.now();

        new StateChanged().fire();

        if (state.get().isEnded()) {
            status = Status.ENDED;
            ended = updated;
            expires = ended.plus(RETENTION_AFTER_ENDED);

            Set<com.wetjens.gwt.Player> winners = state.get().winners();
            players.forEach(player -> {
                var playerInState = state.get().getPlayerByName(player.getId().getId());

                player.setScore(new Score(state.get().score(playerInState).getCategories().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue))));

                player.setWinner(winners.contains(playerInState));
            });

            new Ended().fire();
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

        log.add(new LogEntry(this, userId, LogEntry.Type.ACCEPT, Collections.emptyList()));

        new Accepted(userId).fire();

        afterRespondToInvitation();
    }

    private void afterRespondToInvitation() {
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

        log.add(new LogEntry(this, userId, LogEntry.Type.REJECT, Collections.emptyList()));

        new Rejected(userId).fire();

        afterRespondToInvitation();
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
            throw APIException.badRequest(APIError.GAME_NOT_STARTED_YET);
        }
        return getPlayerById(Player.Id.of(state.get().getCurrentPlayer().getName()));
    }

    public Player getPlayerById(Player.Id playerId) {
        return players.stream()
                .filter(player -> player.getId().equals(playerId))
                .findAny()
                .orElseThrow(() -> APIException.serverError(APIError.NOT_PLAYER_IN_GAME));
    }

    public enum Status {
        NEW,
        STARTED,
        ENDED
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;

        private static Id generate() {
            return of(UUID.randomUUID().toString());
        }
    }

    @Value
    public class Invited implements DomainEvent {
        Game game = Game.this;
        User.Id userId;
    }

    @Value
    public class Accepted implements DomainEvent {
        Game game = Game.this;
        User.Id userId;
    }

    @Value
    public class Rejected implements DomainEvent {
        Game game = Game.this;
        User.Id userId;
    }

    @Value
    public class Started implements DomainEvent {
        Game game = Game.this;
    }

    @Value
    public class Ended implements DomainEvent {
        Game game = Game.this;
    }

    @Value
    public class StateChanged implements DomainEvent {
        Game game = Game.this;
    }

    @Value
    private class Created implements DomainEvent {
        Game game = Game.this;
    }
}
