package com.wetjens.gwt.server.domain;

import com.wetjens.gwt.api.EventListener;
import com.wetjens.gwt.api.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(doNotUseGetters = true)
public class Table {

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
    private final Game game;

    @Getter
    @NonNull
    private Options options;

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

    public static Table create(@NonNull Game game,
                               @NonNull User owner,
                               @NonNull Set<User> inviteUsers,
                               @NonNull Options options) {
        if (inviteUsers.contains(owner)) {
            throw APIException.badRequest(APIError.CANNOT_INVITE_YOURSELF);
        }

        if (inviteUsers.size() > game.getMaxNumberOfPlayers() - 1) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        if (Tables.instance().findByUserId(owner.getId())
                .filter(table -> table.getType() == Type.REALTIME)
                .filter(table -> table.getStatus() == Status.NEW || table.getStatus() == Status.STARTED)
                .count() >= 1) {
            throw APIException.forbidden(APIError.EXCEEDS_MAX_REALTIME_GAMES);
        }

        var player = Player.accepted(owner.getId());

        var created = Instant.now();
        Table table = Table.builder()
                .id(Id.generate())
                .game(game)
                .type(Type.REALTIME)
                .status(Status.NEW)
                .options(options)
                .created(created)
                .updated(created)
                .expires(created.plus(START_TIMEOUT))
                .owner(owner.getId())
                .players(Collections.singleton(player))
                .log(new Log())
                .build();

        table.log.add(new LogEntry(player, LogEntry.Type.CREATE));

        table.new Created().fire();

        inviteUsers.forEach(table::invite);

        return table;
    }

    public void start() {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        players.removeIf(player -> player.getStatus() != Player.Status.ACCEPTED);

        if (players.size() < game.getMinNumberOfPlayers()) {
            throw APIException.badRequest(APIError.MIN_PLAYERS);
        }

        var randomColors = new LinkedList<>(game.getAvailableColors());
        Collections.shuffle(randomColors, RANDOM);

        players.forEach(player -> player.setColor(randomColors.poll()));

        state = Lazy.of(game.start(players.stream()
                .map(player -> new com.wetjens.gwt.api.Player(player.getId().getId(), player.getColor()))
                .collect(Collectors.toSet()), options, RANDOM));

        afterStateChange();

        status = Status.STARTED;
        started = Instant.now();
        updated = started;
        expires = started.plus(ACTION_TIMEOUT);

        log.add(new LogEntry(getPlayerByUserId(owner).orElseThrow(), LogEntry.Type.START));

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

        runStateChange(() -> game.executeAutoma(state.get(), RANDOM));
    }

    private void runStateChange(Runnable runnable) {
        var state = this.state.get();

        EventListener eventListener = event -> log.add(new LogEntry(this, event));
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

        var player = getPlayerByUserId(userId)
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));


        if (owner.equals(userId)) {
            // if owner wants to leave, have to appoint a new owner
            otherHumanPlayers(userId)
                    .filter(Player::hasAccepted)
                    .findAny()
                    .map(Player::getUserId)
                    .ifPresentOrElse(this::changeOwner, this::abandon);
        }

        player.leave();

        players.remove(player);
        new Left(id, userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.LEFT));

        updated = Instant.now();

        if (status == Status.STARTED) {
            // TODO How to continue the game if player leaves
            if (players.size() > game.getMinNumberOfPlayers()) {
                // Game is still able to continue with one less player
                runStateChange(() -> state.get().leave(state.get().getPlayerByName(player.getId().getId())));
            } else {
                // Game cannot be continued without player
                abandon();
            }

            // TODO Deduct karma points if playing with humans
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

        var player = getPlayerByUserId(userId)
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));

        player.proposeToLeave();

        log.add(new LogEntry(player, LogEntry.Type.PROPOSED_TO_LEAVE));

        new ProposedToLeave(id, userId).fire();
    }

    public void agreeToLeave(User.Id userId) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var agreeingPlayer = getPlayerByUserId(userId)
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));

        agreeingPlayer.agreeToLeave();

        new AgreedToLeave(id, userId).fire();

        log.add(new LogEntry(agreeingPlayer, LogEntry.Type.AGREED_TO_LEAVE));

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
        new Accepted(userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.ACCEPT));

        updated = Instant.now();

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

        players.remove(player);
        new Rejected(userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.REJECT));

        updated = Instant.now();

        afterRespondToInvitation();
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean canStart() {
        return status == Status.NEW && playersThatAccepted().count() >= MIN_NUMBER_OF_PLAYERS;
    }

    public Optional<Player> getPlayerByUserId(User.Id userId) {
        return players.stream()
                .filter(player -> userId.equals(player.getUserId()))
                .findAny();
    }

    public Player getCurrentPlayer() {
        if (state == null) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }
        return getPlayerById(Player.Id.of(state.get().getCurrentPlayer().getName()))
                .orElseThrow(() -> APIException.internalError(APIError.NOT_PLAYER_IN_GAME));
    }

    public Optional<Player> getPlayerById(Player.Id playerId) {
        return players.stream()
                .filter(player -> player.getId().equals(playerId))
                .findAny();
    }

    public void invite(User user) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        if (players.size() == game.getMaxNumberOfPlayers()) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        if (players.stream().anyMatch(player -> user.getId().equals(player.getUserId()))) {
            throw APIException.badRequest(APIError.ALREADY_INVITED);
        }

        var player = Player.invite(user.getId());
        players.add(player);

        log.add(new LogEntry(player, LogEntry.Type.INVITE, List.of(user.getUsername())));

        new Invited(user.getId()).fire();
    }

    public void uninvite(User user) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        var player = getPlayerByUserId(user.getId())
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));

        players.remove(player);

        log.add(new LogEntry(player, LogEntry.Type.UNINVITE, List.of(user.getUsername())));

        new Uninvited(user.getId()).fire();
    }

    public enum Status {
        NEW,
        STARTED,
        ABANDONED,
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
        Table table = Table.this;
        User.Id userId;
    }

    @Value
    public class Uninvited implements DomainEvent {
        Table table = Table.this;
        User.Id userId;
    }

    @Value
    public class Accepted implements DomainEvent {
        Table table = Table.this;
        User.Id userId;
    }

    @Value
    public class Rejected implements DomainEvent {
        Table table = Table.this;
        User.Id userId;
    }

    @Value
    public class Started implements DomainEvent {
        Table table = Table.this;
    }

    @Value
    public class Ended implements DomainEvent {
        Table table = Table.this;
    }

    @Value
    public class StateChanged implements DomainEvent {
        Table table = Table.this;
    }

    @Value
    private class Created implements DomainEvent {
        Table table = Table.this;
    }

    @Value
    private static class ChangedOwner implements DomainEvent {
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class Left implements DomainEvent {
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class ProposedToLeave implements DomainEvent {
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class AgreedToLeave implements DomainEvent {
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    public enum Type {
        REALTIME
    }

    @Value
    public class Abandoned implements DomainEvent {
        @NonNull Table.Id tableId;
    }
}
