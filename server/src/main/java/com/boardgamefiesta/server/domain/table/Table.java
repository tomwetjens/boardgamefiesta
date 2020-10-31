package com.boardgamefiesta.server.domain.table;

import com.boardgamefiesta.api.domain.EventListener;
import com.boardgamefiesta.api.domain.*;
import com.boardgamefiesta.server.domain.APIError;
import com.boardgamefiesta.server.domain.APIException;
import com.boardgamefiesta.server.domain.DomainEvent;
import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.user.User;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(doNotUseGetters = true)
public class Table {

    private static final Duration RETENTION_NEW = Duration.of(2, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ACTION = Duration.of(14, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ENDED = Duration.of(365 * 5, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ABANDONED = Duration.of(1, ChronoUnit.DAYS);
    private static final TemporalAmount RETENTION_HISTORIC_STATE = RETENTION_AFTER_ENDED;

    private static final Random RANDOM = new Random();

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

    @Getter
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
    private Optional<CurrentState> currentState;

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
                               @NonNull Options options) {
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
                .players(new HashSet<>(Collections.singleton(player)))
                .log(new Log())
                .currentState(Optional.empty())
                .build();

        table.log.add(new LogEntry(player, LogEntry.Type.CREATE));

        new Created(table.getId()).fire();

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

        CurrentState initialState = CurrentState.initial(game.start(players.stream()
                .map(player -> new com.boardgamefiesta.api.domain.Player(player.getId().getId(), player.getColor()))
                .collect(Collectors.toSet()), options, RANDOM));
        currentState = Optional.of(initialState);
        historicStates = HistoricStates.initial(initialState);

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
            runStateChange(state -> state.perform(action, RANDOM));
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

        runStateChange(state -> game.executeAutoma(state, RANDOM));
    }

    private void runStateChange(Consumer<State> change) {
        var beginState = this.currentState.orElseThrow(() -> APIException.internalError(APIError.GAME_NOT_STARTED));
        var state = beginState.getState();

        var currentPlayer = getCurrentPlayer();

        EventListener eventListener = event -> log.add(new LogEntry(this, event));
        state.addEventListener(eventListener);
        try {
            change.accept(state);
        } finally {
            state.removeEventListener(eventListener);
        }

        CurrentState newState = beginState.next(state);

        currentState = Optional.of(newState);
        historicStates.add(HistoricState.from(newState));

        afterStateChange();

        // TODO Move this into afterStateChange to also support undoing after turn ends
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
            runStateChange(state -> state.skip(RANDOM));
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
            runStateChange(state -> state.endTurn(RANDOM));
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
            otherUsersPlaying(userId)
                    .findAny()
                    .flatMap(Player::getUserId)
                    .ifPresentOrElse(this::changeOwner, this::abandon);
        }

        log.add(new LogEntry(player, LogEntry.Type.LEFT));

        player.leave();

        if (status == Status.STARTED) {
            if (players.stream().filter(Player::isPlaying).count() >= game.getMinNumberOfPlayers()) {
                // Game is still able to continue with one less player
                runStateChange(state -> state.leave(state.getPlayerByName(player.getId().getId()).orElseThrow()));
            } else {
                // Game cannot be continued without player
                abandon();
            }

            // TODO Deduct karma points if playing with humans
        }

        new Left(id, userId).fire();


        updated = Instant.now();
    }

    private Stream<Player> otherUsersPlaying(User.Id userId) {
        return players.stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .filter(Player::isPlaying)
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

        if (otherUsersPlaying(userId).allMatch(Player::hasAgreedToLeave)) {
            abandon();
        }
    }

    private void afterStateChange() {
        updated = Instant.now();

        new StateChanged(id).fire();

        var currentState = this.currentState.orElseThrow();
        var state = currentState.getState();

        if (state.isEnded()) {
            status = Status.ENDED;
            ended = updated;
            expires = ended.plus(RETENTION_AFTER_ENDED);

            var winners = state.winners();

            for (Player player : players) {
                state.getPlayerByName(player.getId().getId()).ifPresent(playerInState ->
                        state.score(playerInState).ifPresent(score ->
                                player.assignScore(score, winners.contains(playerInState))));
            }

            new Ended(id).fire();
        } else {
            expires = updated.plus(RETENTION_AFTER_ACTION);

            for (Player player : players) {
                state.getPlayerByName(player.getId().getId())
                        .flatMap(state::score)
                        .ifPresent(score -> player.assignScore(score, false));
            }
        }
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

        if (status != Status.STARTED && otherUsersPlaying(ownerId).count() > 1) {
            throw APIException.forbidden(APIError.CANNOT_ABANDON);
        }

        status = Status.ABANDONED;
        updated = Instant.now();
        expires = Instant.now().plus(RETENTION_AFTER_ABANDONED);

        new Abandoned(id).fire();
    }

    private Stream<Player> playersThatAccepted() {
        return players.stream().filter(player -> player.getStatus() == Player.Status.ACCEPTED);
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

    public boolean canStart() {
        return status == Status.NEW && playersThatAccepted().count() >= game.getMinNumberOfPlayers();
    }

    public Optional<Player> getPlayerByUserId(User.Id userId) {
        return players.stream()
                .filter(player -> userId.equals(player.getUserId().orElse(null)))
                .findAny();
    }

    public Player getCurrentPlayer() {
        var state = this.currentState.map(CurrentState::getState)
                .orElseThrow(() -> APIException.badRequest(APIError.GAME_NOT_STARTED));

        return getPlayerById(Player.Id.of(state.getCurrentPlayer().getName()))
                .orElseThrow(() -> APIException.internalError(APIError.NOT_PLAYER_IN_GAME));
    }

    public State getState() {
        return currentState.orElseThrow(() -> APIException.badRequest(APIError.GAME_NOT_STARTED)).getState();
    }

    public Optional<Player> getPlayerById(Player.Id playerId) {
        return players.stream()
                .filter(player -> playerId.equals(player.getId()))
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

        var previousState = this.currentState
                .orElseThrow(() -> APIException.internalError(APIError.GAME_NOT_STARTED));

        this.currentState = Optional.of(previousState.revertTo(historicState));

        afterStateChange();
    }

    public void undo() {
        if (status != Status.STARTED) {
            throw APIException.badRequest(APIError.GAME_NOT_STARTED);
        }

        var currentState = this.currentState
                .orElseThrow(() -> APIException.internalError(APIError.GAME_NOT_STARTED));
        var currentPlayer = getCurrentPlayer();

        if (!currentState.canUndo()) {
            throw APIException.badRequest(APIError.CANNOT_UNDO);
        }

        var historicState = currentState.getPrevious()
                .flatMap(historicStates::get)
                .orElseThrow(() -> APIException.badRequest(APIError.HISTORY_NOT_AVAILABLE));

        log.add(new LogEntry(currentPlayer, LogEntry.Type.UNDO));

        this.currentState = Optional.of(currentState.revertTo(historicState));

        afterStateChange();
    }

    public boolean canUndo() {
        return currentState.map(CurrentState::canUndo).orElse(false);
    }

    public boolean isActive() {
        return status != Status.ENDED && status != Status.ABANDONED;
    }

    public boolean hasComputerPlayers() {
        return players.stream().anyMatch(player -> player.getType() == Player.Type.COMPUTER);
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

    @Value(staticConstructor = "of")
    public static class CurrentState {
        State state;
        Instant timestamp;
        Optional<Instant> previous;

        public static CurrentState initial(State state) {
            return new CurrentState(state, Instant.now(), Optional.empty());
        }

        public CurrentState next(State state) {
            return new CurrentState(state, Instant.now(), Optional.of(timestamp));
        }

        public CurrentState revertTo(HistoricState historicState) {
            return new CurrentState(historicState.getState(), historicState.getTimestamp(), historicState.getPrevious());
        }

        public boolean canUndo() {
            return previous.isPresent() && state.canUndo();
        }
    }

    @Value(staticConstructor = "of")
    public static class HistoricState {
        Instant timestamp;
        Optional<Instant> previous;
        State state;
        Instant expires;

        public static HistoricState from(CurrentState currentState) {
            return new HistoricState(currentState.getTimestamp(), currentState.getPrevious(), currentState.getState(), Instant.now().plus(RETENTION_HISTORIC_STATE));
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HistoricStates {

        private final Set<HistoricState> pending = new HashSet<>();

        private final NavigableMap<Instant, HistoricState> states;

        /**
         * Supplies Historic States that occurred before the given timestamp, most recent first.
         */
        private final Function<Instant, Optional<HistoricState>> supplier;

        public static HistoricStates initial(CurrentState currentState) {
            HistoricStates historicStates = new HistoricStates(new TreeMap<>(), timestamp -> Optional.empty());
            historicStates.add(HistoricState.from(currentState));
            return historicStates;
        }

        public static HistoricStates of(HistoricState... historicStates) {
            return new HistoricStates(new TreeMap<>(Arrays.stream(historicStates)
                    .collect(Collectors.toMap(HistoricState::getTimestamp, Function.identity()))), timestamp -> Optional.empty());
        }

        public static HistoricStates defer(Function<Instant, Optional<HistoricState>> supplier) {
            return new HistoricStates(new TreeMap<>(), supplier);
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
            if (historicState == null && supplier != null) {
                historicState = supplier.apply(timestamp).orElse(null);
                if (historicState != null) {
                    states.put(historicState.getTimestamp(), historicState);
                }
            }
            return Optional.ofNullable(historicState);
        }

    }
}
