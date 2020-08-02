package com.boardgamefiesta.server.domain;

import com.boardgamefiesta.api.domain.EventListener;
import com.boardgamefiesta.api.domain.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(doNotUseGetters = true)
public class Table {

    private static final Duration RETENTION_NEW = Duration.of(2, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ACTION = Duration.of(1, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ENDED = Duration.of(5, ChronoUnit.YEARS);
    private static final Duration RETENTION_AFTER_ABANDONED = Duration.of(1, ChronoUnit.HOURS);

    private static final Random RANDOM = new Random();
    private static final int MIN_NUMBER_OF_PLAYERS = 2;
    private static final int MAX_NUMBER_OF_PLAYERS = 4;

    @Getter
    @NonNull
    private final Id id;

    // TODO Nullable for backwards compatibility, make int
    @Getter
    private final Integer version;

    @Getter
    @NonNull
    private final Type type;

    @Getter
    @NonNull
    private final Mode mode;

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
    private User.Id ownerId;

    @Getter
    private CurrentState state;

    @Getter
    private HistoricStates historicStates;

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
                               @NonNull Mode mode,
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
                .version(1)
                .game(game)
                .type(Type.REALTIME)
                .mode(mode)
                .status(Status.NEW)
                .options(options)
                .created(created)
                .updated(created)
                .expires(created.plus(RETENTION_NEW))
                .ownerId(owner.getId())
                .players(Collections.singleton(player))
                .log(new Log())
                .build();

        table.log.add(new LogEntry(player, LogEntry.Type.CREATE));

        new Created(table.getId()).fire();

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

        var randomColors = new ArrayList<>(game.getSupportedColors());
        Collections.shuffle(randomColors, RANDOM);

        players.forEach(player -> player.assignColor(randomColors.remove(randomColors.size() - 1)));

        status = Status.STARTED;
        started = Instant.now();
        updated = started;
        expires = started.plus(RETENTION_AFTER_ACTION);

        state = CurrentState.of(game.start(players.stream()
                .map(player -> new com.boardgamefiesta.api.domain.Player(player.getId().getId(), player.getColor()))
                .collect(Collectors.toSet()), options, RANDOM));
        historicStates = HistoricStates.initial();

        afterStateChange();

        log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.START));

        new Started(id).fire();

        getCurrentPlayer().beginTurn(game.getTimeLimit(options));
    }

    public void perform(Action action) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        try {
            runStateChange(() -> state.get().perform(action, RANDOM));
        } catch (InGameException e) {
            throw APIException.inGame(e, game.getId());
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

            if (newCurrentPlayer != currentPlayer) {
                currentPlayer.endTurn();

                if (newCurrentPlayer != null) {
                    newCurrentPlayer.beginTurn(game.getTimeLimit(options));
                }
            }
        } else {
            currentPlayer.endTurn();
        }
    }

    public void skip() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        try {
            runStateChange(() -> state.get().skip(RANDOM));
        } catch (InGameException e) {
            throw APIException.inGame(e, game.getId());
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
            throw APIException.inGame(e, game.getId());
        }

        player.endTurn();
    }

    public void leave(@NonNull User.Id userId) {
        if (status == Status.ENDED) {
            throw APIException.badRequest(APIError.GAME_ALREADY_ENDED);
        }

        if (status == Status.ABANDONED) {
            throw APIException.badRequest(APIError.GAME_ABANDONED);
        }

        var player = getPlayerByUserId(userId)
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));

        if (ownerId.equals(userId)) {
            // if owner wants to leave, have to appoint a new owner
            otherHumanPlayers(userId)
                    .filter(Player::hasAccepted)
                    .findAny()
                    .flatMap(Player::getUserId)
                    .ifPresentOrElse(this::changeOwner, this::abandon);
        }

        player.leave();

        players.remove(player);
        new Left(id, userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.LEFT));

        updated = Instant.now();

        if (status == Status.STARTED) {
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
                .filter(player -> !player.getUserId().orElseThrow().equals(userId));
    }

    private void changeOwner(User.Id userId) {
        ownerId = userId;
        updated = Instant.now();

        new ChangedOwner(id, userId).fire();
    }

    public void proposeToLeave(@NonNull User.Id userId) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var player = getPlayerByUserId(userId)
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));

        player.proposeToLeave();

        log.add(new LogEntry(player, LogEntry.Type.PROPOSED_TO_LEAVE));

        new ProposedToLeave(id, userId).fire();
    }

    public void agreeToLeave(@NonNull User.Id userId) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var player = getPlayerByUserId(userId)
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_PLAYER_IN_GAME));

        player.agreeToLeave();

        new AgreedToLeave(id, userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.AGREED_TO_LEAVE));

        updated = Instant.now();

        var allOtherPlayersAgreedToLeave = players.stream()
                .filter(otherPlayer -> otherPlayer.getType() == Player.Type.USER)
                .filter(otherPlayer -> !userId.equals(otherPlayer.getUserId().orElseThrow()))
                .allMatch(Player::hasAgreedToLeave);

        if (allOtherPlayersAgreedToLeave) {
            abandon();
        }
    }

    private void afterStateChange() {
        updated = Instant.now();

        new StateChanged(id).fire();

        if (state.get().isEnded()) {
            status = Status.ENDED;
            ended = updated;
            expires = ended.plus(RETENTION_AFTER_ENDED);

            var winners = state.get().winners();

            for (Player player : players) {
                var playerInState = state.get().getPlayerByName(player.getId().getId());

                var score = state.get().score(playerInState);
                var winner = winners.contains(playerInState);

                player.assignScore(score, winner);
            }

            new Ended(id).fire();
        } else {
            expires = updated.plus(RETENTION_AFTER_ACTION);

            for (Player player : players) {
                var playerInState = state.get().getPlayerByName(player.getId().getId());

                var score = state.get().score(playerInState);

                player.assignScore(score, false);
            }
        }

        // TODO Determine correct expiry for historic states
        historicStates.add(new HistoricState(updated, state.get(), expires));
    }

    public void acceptInvite(@NonNull User.Id userId) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        var player = players.stream()
                .filter(p -> userId.equals(p.getUserId().orElse(null)))
                .findAny()
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_INVITED));

        player.accept();
        new Accepted(id, userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.ACCEPT));

        updated = Instant.now();
    }

    public void abandon() {
        if (status != Status.NEW && status != Status.STARTED) {
            throw APIException.badRequest(APIError.CANNOT_ABANDON);
        }

        if (status != Status.STARTED && otherHumanPlayers(ownerId).count() > 1) {
            throw APIException.forbidden(APIError.CANNOT_ABANDON);
        }

        status = Status.ABANDONED;
        updated = Instant.now();
        expires = Instant.now().plus(RETENTION_AFTER_ABANDONED);

        new Abandoned(id).fire();
    }

    private Stream<Player> playersThatAccepted() {
        return players.stream().filter(Player::hasAccepted);
    }

    public void rejectInvite(@NonNull User.Id userId) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        var player = players.stream()
                .filter(p -> userId.equals(p.getUserId().orElse(null)))
                .findAny()
                .orElseThrow(() -> APIException.badRequest(APIError.NOT_INVITED));

        player.reject();

        players.remove(player);
        new Rejected(id, userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.REJECT));

        updated = Instant.now();
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean canStart() {
        return status == Status.NEW && playersThatAccepted().count() >= MIN_NUMBER_OF_PLAYERS;
    }

    public Optional<Player> getPlayerByUserId(User.Id userId) {
        return players.stream()
                .filter(player -> userId.equals(player.getUserId().orElse(null)))
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

        if (players.stream().anyMatch(player -> user.getId().equals(player.getUserId().orElse(null)))) {
            throw APIException.badRequest(APIError.ALREADY_INVITED);
        }

        var player = Player.invite(user.getId());
        players.add(player);

        log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.INVITE, List.of(user.getId().getId())));

        new Invited(id, user.getId()).fire();
    }

    public void kick(Player player) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }
        // TODO With enough time passed, or votes, a player can be removed after the game has started

        if (!players.remove(player)) {
            throw APIException.badRequest(APIError.NOT_PLAYER_IN_GAME);
        }

        if (player.getType() == Player.Type.USER) {
            var userId = player.getUserId().orElseThrow();

            log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.KICK, List.of(userId.getId())));

            new Kicked(this.id, userId).fire();
        }
    }

    public void addComputer() {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        if (players.size() == game.getMaxNumberOfPlayers()) {
            throw APIException.badRequest(APIError.EXCEEDS_MAX_PLAYERS);
        }

        if (!game.hasAutoma()) {
            throw APIException.badRequest(APIError.COMPUTER_NOT_SUPPORTED);
        }

        players.add(Player.computer());

        new ComputerAdded(id);
    }

    public void changeOptions(@NonNull Options options) {
        if (status != Status.NEW) {
            throw APIException.badRequest(APIError.GAME_ALREADY_STARTED_OR_ENDED);
        }

        this.options = options;

        new OptionsChanged(id).fire();
    }

    public void revertTo(Instant timestamp) {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        if (mode != Mode.TRAINING) {
            throw APIException.badRequest(APIError.NOT_TRAINING_MODE);
        }

        var historicState = historicStates.get(timestamp)
                .orElseThrow(() -> APIException.badRequest(APIError.HISTORY_NOT_AVAILABLE));

        state = CurrentState.of(historicState.getState());

        afterStateChange();
    }

    public enum Status {
        NEW,
        STARTED,
        ABANDONED,
        ENDED
    }

    public enum Type {
        REALTIME
    }

    public enum Mode {
        NORMAL,
        TRAINING
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;

        private static Id generate() {
            return of(UUID.randomUUID().toString());
        }
    }

    @Value
    public static class Invited implements DomainEvent {
        Table.Id tableId;
        User.Id userId;
    }

    @Value
    public static class Kicked implements DomainEvent {
        Table.Id tableId;
        User.Id userId;
    }

    @Value
    public static class Accepted implements DomainEvent {
        Table.Id tableId;
        User.Id userId;
    }

    @Value
    public static class Rejected implements DomainEvent {
        Table.Id tableId;
        User.Id userId;
    }

    @Value
    public static class Started implements DomainEvent {
        Table.Id tableId;
    }

    @Value
    public static class Ended implements DomainEvent {
        Table.Id tableId;
    }

    @Value
    public static class StateChanged implements DomainEvent {
        Table.Id tableId;
    }

    @Value
    public static class Created implements DomainEvent {
        Table.Id tableId;
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

    @Value
    public static class Abandoned implements DomainEvent {
        @NonNull Table.Id tableId;
    }

    @Value
    public static class ComputerAdded implements DomainEvent {
        @NonNull Table.Id tableId;
    }

    @Value
    public static class OptionsChanged implements DomainEvent {
        @NonNull Table.Id tableId;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class CurrentState {

        private final Supplier<State> supplier;
        private State current;

        public static CurrentState of(State state) {
            return new CurrentState(null, state);
        }

        public static CurrentState defer(Supplier<State> supplier) {
            return new CurrentState(supplier, null);
        }

        public State get() {
            if (current == null) {
                current = supplier.get();
            }
            return current;
        }

        public boolean isPresent() {
            return current != null;
        }

    }

    @Value
    public static class HistoricState {
        Instant timestamp;
        State state;
        Instant expires;
    }

    @RequiredArgsConstructor(staticName = "defer")
    public static class HistoricStates {

        private final Function<Instant, Optional<HistoricState>> supplier;

        private final NavigableMap<Instant, HistoricState> states = new TreeMap<>();
        private final Set<HistoricState> pending = new HashSet<>();

        public static HistoricStates initial() {
            return new HistoricStates(timestamp -> Optional.empty());
        }

        public void add(HistoricState historicState) {
            states.put(historicState.getTimestamp(), historicState);
            pending.add(historicState);
        }

        public Set<HistoricState> getPending() {
            return Collections.unmodifiableSet(pending);
        }

        public void commit() {
            pending.clear();
        }

        public boolean hasPending() {
            return !pending.isEmpty();
        }

        public Optional<HistoricState> get(Instant timestamp) {
            var historicState = states.get(timestamp);
            if (historicState == null) {
                historicState = supplier.apply(timestamp).orElse(null);
                if (historicState != null) {
                    states.put(historicState.getTimestamp(), historicState);
                }
            }
            return Optional.ofNullable(historicState);
        }
    }

}
