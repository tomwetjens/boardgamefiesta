package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.api.Action;
import com.wetjens.gwt.api.EventListener;
import com.wetjens.gwt.api.Implementation;
import com.wetjens.gwt.api.InGameException;
import com.wetjens.gwt.api.State;
import com.wetjens.gwt.server.rest.APIError;
import com.wetjens.gwt.server.rest.APIException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private static final Random RANDOM = new Random();
    private static final int MIN_NUMBER_OF_PLAYERS = 2;
    private static final int MAX_NUMBER_OF_PLAYERS = 4;

    @Getter
    @NonNull
    private final Id id;

    @Getter
    @NonNull
    private final Type type;

    @Getter
    @NonNull
    private Implementation implementation;

    @Getter
    @NonNull
    private Map<String, String> options;

    @Getter
    @NonNull
    private final Instant created;

    @NonNull
    private final Set<Player> players;

    @Getter
    @NonNull
    private final Log log;

    @Getter
    @NonNull
    private Status status;

    @Getter
    @NonNull
    private User.Id owner;

    @Getter
    private Lazy<State> state;

    @Getter
    @NonNull
    private Instant expires;

    @Getter
    private Instant updated;

    @Getter
    private Instant started;

    @Getter
    private Instant ended;

    public static Game create(@NonNull Implementation implementation,
                              @NonNull User owner,
                              @NonNull Set<User> inviteUsers,
                              @NonNull Map<String, String> options) {
        if (inviteUsers.contains(owner)) {
            throw APIException.badRequest(APIError.CANNOT_INVITE_YOURSELF);
        }

        if (inviteUsers.size() > implementation.getMaxNumberOfPlayers() - 1) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        Games games = Games.instance();
        if (games.countByUserId(owner.getId()) >= 1) {
            throw APIException.forbidden(APIError.EXCEEDS_MAX_REALTIME_GAMES);
        }

        var player = Player.accepted(owner.getId());
        var invitedPlayers = inviteUsers.stream().map(user -> Player.invite(user.getId()));

        var created = Instant.now();
        Game game = Game.builder()
                .id(Id.generate())
                .implementation(implementation)
                .type(Type.REALTIME)
                .status(Status.NEW)
                .options(options)
                .created(created)
                .updated(created)
                .expires(created.plus(START_TIMEOUT))
                .owner(owner.getId())
                .players(Stream.concat(Stream.of(player), invitedPlayers).collect(Collectors.toSet()))
                .log(new Log())
                .build();

        game.log.add(new LogEntry(player, LogEntry.Type.CREATE, Collections.emptyList()));
        inviteUsers.forEach(invitedUser -> game.log.add(new LogEntry(player, LogEntry.Type.INVITE, List.of(invitedUser.getUsername()))));

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

        if (players.size() < implementation.getMinNumberOfPlayers()) {
            throw APIException.badRequest(APIError.MIN_PLAYERS);
        }

        var randomColors = new LinkedList<>(implementation.getAvailableColors());
        Collections.shuffle(randomColors, RANDOM);

        players.forEach(player -> player.setColor(randomColors.poll()));

        state = Lazy.of(implementation.start(players.stream()
                .map(player -> new com.wetjens.gwt.api.Player(player.getId().getId(), player.getColor()))
                .collect(Collectors.toSet()), options, RANDOM));

        afterStateChange();

        status = Status.STARTED;
        started = Instant.now();
        updated = started;
        expires = started.plus(ACTION_TIMEOUT);

        log.add(new LogEntry(getPlayerByUserId(owner), LogEntry.Type.START, Collections.emptyList()));

        new Started().fire();

        getCurrentPlayer().beginTurn();
    }

    public void perform(Action action) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        try {
            runStateChange(() -> state.get().perform(action, RANDOM));
        } catch (InGameException e) {
            throw new APIException(e);
        }
    }

    public void executeAutoma() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var currentPlayer = getCurrentPlayer();
        if (currentPlayer.getType() != Player.Type.COMPUTER) {
            throw new IllegalStateException("Current player is not computer");
        }

        runStateChange(() -> implementation.executeAutoma(state.get(), RANDOM));
    }

    private void runStateChange(Runnable runnable) {
        var state = this.state.get();

        EventListener eventListener = event -> log.add(new LogEntry(event));
        state.addEventListener(eventListener);

        var currentPlayer = getCurrentPlayer();

        runnable.run();

        state.removeEventListener(eventListener);

        afterStateChange();

        if (!state.isEnded()) {
            var newCurrentPlayer = getCurrentPlayer();

            if (newCurrentPlayer != null) {
                currentPlayer.endTurn();
                newCurrentPlayer.beginTurn();
            }
        }
    }

    public void skip() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        try {
            runStateChange(() -> state.get().skip(RANDOM));
        } catch (InGameException e) {
            throw new APIException(e);
        }
    }

    public void endTurn() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var player = getCurrentPlayer();

        try {
            runStateChange(() -> state.get().endTurn(RANDOM));
        } catch (InGameException e) {
            throw new APIException(e);
        }

        player.endTurn();
    }

    public void leave(User.Id userId) {
        if (status == Status.ENDED) {
            throw APIException.badRequest(APIError.GAME_ALREADY_ENDED);
        }

        if (status == Status.ABANDONED) {
            throw APIException.badRequest(APIError.GAME_ABANDONED);
        }

        if (owner.equals(userId)) {
            // if owner wants to leave, have to appoint a new owner
            otherHumanPlayers(userId)
                    .filter(player -> player.getStatus() == Player.Status.ACCEPTED)
                    .findAny()
                    .map(Player::getUserId)
                    .ifPresentOrElse(this::changeOwner, this::abandon);
        }

        getPlayerByUserId(userId).leave();

        new Left(id, userId).fire();

        if (status == Status.STARTED) {
            // TODO How to continue the game if player leaves

            // TODO Deduct karma points
        }
    }

    private Stream<Player> otherHumanPlayers(User.Id userId) {
        return players.stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .filter(player -> !player.getUserId().equals(userId));
    }

    private void changeOwner(User.Id userId) {
        owner = userId;
        updated = Instant.now();

        new ChangedOwner(id, userId).fire();
    }

    public void proposeToLeave(User.Id userId) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var player = getPlayerByUserId(userId);

        player.proposeToLeave();

        log.add(new LogEntry(player, LogEntry.Type.PROPOSED_TO_LEAVE));

        new ProposedToLeave(id, userId).fire();
    }

    public void agreeToLeave(User.Id userId) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var agreeingPlayer = getPlayerByUserId(userId);

        agreeingPlayer.agreeToLeave();

        log.add(new LogEntry(agreeingPlayer, LogEntry.Type.AGREED_TO_LEAVE));

        new AgreedToLeave(id, userId).fire();

        updated = Instant.now();

        var allOtherPlayersAgreedToLeave = players.stream()
                .filter(player -> !userId.equals(player.getUserId()))
                .filter(otherPlayer -> otherPlayer.getType() == Player.Type.USER)
                .allMatch(Player::hasAgreedToLeave);

        if (allOtherPlayersAgreedToLeave) {
            abandon();
        }
    }

    private void afterStateChange() {
        updated = Instant.now();

        new StateChanged().fire();

        if (state.get().isEnded()) {
            status = Status.ENDED;
            ended = updated;
            expires = ended.plus(RETENTION_AFTER_ENDED);

            Set<com.wetjens.gwt.api.Player> winners = state.get().winners();
            players.forEach(player -> {
                var playerInState = state.get().getPlayers().stream()
                        .filter(p -> p.getName().equals(player.getId().getId()))
                        .findAny()
                        .orElseThrow();

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

        log.add(new LogEntry(player, LogEntry.Type.ACCEPT, Collections.emptyList()));

        new Accepted(userId).fire();

        afterRespondToInvitation();
    }

    private void afterRespondToInvitation() {
        if (allPlayersResponded()) {
            if ((int) playersThatAccepted().count() >= 2) {
                // If enough players have accepted, automatically start
                start();
            }
        }
    }

    public void abandon(User.Id userId) {
        if (!owner.equals(userId)) {
            throw APIException.forbidden(APIError.MUST_BE_OWNER);
        }

        if (otherHumanPlayers(userId).count() > 1) {
            throw APIException.forbidden(APIError.CANNOT_ABANDON);
        }

        abandon();
    }

    private void abandon() {
        status = Status.ABANDONED;
        updated = Instant.now();
        expires = Instant.now();

        new Abandoned(id).fire();
    }

    private Stream<Player> playersThatAccepted() {
        return players.stream().filter(Player::hasAccepted);
    }

    private boolean allPlayersResponded() {
        return players.stream().allMatch(Player::hasResponded);
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

        log.add(new LogEntry(player, LogEntry.Type.REJECT, Collections.emptyList()));

        new Rejected(userId).fire();

        afterRespondToInvitation();
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean canStart() {
        return status == Status.NEW && playersThatAccepted().count() >= MIN_NUMBER_OF_PLAYERS;
    }

    public Player getPlayerByUserId(User.Id userId) {
        return players.stream()
                .filter(player -> userId.equals(player.getUserId()))
                .findAny()
                .orElseThrow(() -> APIException.forbidden(APIError.NOT_PLAYER_IN_GAME));
    }

    public Player getCurrentPlayer() {
        if (state == null) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
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
        ABANDONED, ENDED
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

    @Value
    private static class ChangedOwner implements DomainEvent {
        @NonNull Game.Id gameId;
        @NonNull User.Id userId;
    }

    @Value
    private static class Left implements DomainEvent {
        @NonNull Game.Id gameId;
        @NonNull User.Id userId;
    }

    @Value
    private static class ProposedToLeave implements DomainEvent {
        @NonNull Game.Id gameId;
        @NonNull User.Id userId;
    }

    @Value
    private static class AgreedToLeave implements DomainEvent {
        @NonNull Game.Id gameId;
        @NonNull User.Id userId;
    }

    public enum Type {
        REALTIME
    }

    @Value
    private class Abandoned implements DomainEvent {
        @NonNull Game.Id gameId;
    }
}
