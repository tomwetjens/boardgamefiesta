package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.api.domain.EventListener;
import com.boardgamefiesta.api.domain.*;
import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.DomainEvent;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.User;
import lombok.*;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(doNotUseGetters = true)
public class Table implements AggregateRoot {

    private static final Duration RETENTION_NEW = Duration.of(2, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ENDED = Duration.of(365 * 5, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ABANDONED = Duration.of(1, ChronoUnit.DAYS);

    private static final SecureRandom RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstance("NativePRNGNonBlocking");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not initialize PRNG", e);
        }
    }

    @Getter
    @NonNull
    private final Id id;

    // TODO Nullable for backwards compatibility, make int
    @Getter
    private final Integer version;

    @Getter
    @NonNull
    private Type type;

    @Getter
    @NonNull
    private Mode mode;

    @Getter
    @NonNull
    private Visibility visibility;

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
    private Lazy<Optional<CurrentState>> currentState;

    @Getter
    private Instant updated;

    @Getter
    private Instant started;

    @Getter
    private Instant ended;

    public static Table create(@NonNull Game game,
                               @NonNull Mode mode,
                               @NonNull User.Id ownerId,
                               @NonNull Options options) {
        var player = Player.accepted(ownerId);

        var created = Instant.now();
        Table table = Table.builder()
                .id(Id.generate())
                .version(1)
                .game(game)
                .type(Type.REALTIME)
                .mode(mode)
                .visibility(Visibility.PRIVATE)
                .status(Status.NEW)
                .options(options)
                .created(created)
                .updated(created)
                .ownerId(ownerId)
                .players(new HashSet<>(Collections.singleton(player)))
                .log(new Log())
                .currentState(Lazy.of(Optional.empty()))
                .build();

        table.log.add(new LogEntry(player, LogEntry.Type.CREATE));

        new Created(table.getId()).fire();

        return table;
    }

    public void start() {
        checkNew();

        players.removeIf(player -> player.getStatus() != Player.Status.ACCEPTED);

        if (players.size() < game.getMinNumberOfPlayers()) {
            throw new NotEnoughPlayers();
        }

        var randomColors = new ArrayList<>(game.getSupportedColors());
        Collections.shuffle(randomColors, RANDOM);

        players.forEach(player -> player.assignColor(randomColors.remove(randomColors.size() - 1)));

        status = Status.STARTED;
        started = Instant.now();
        updated = started;

        try {
            var state = game.start(players.stream()
                    .map(player -> new com.boardgamefiesta.api.domain.Player(player.getId().getId(), player.getColor(),
                            player.getType() == Player.Type.COMPUTER
                                    ? com.boardgamefiesta.api.domain.Player.Type.COMPUTER
                                    : com.boardgamefiesta.api.domain.Player.Type.HUMAN))
                    .collect(Collectors.toSet()), options, RANDOM);

            currentState = Lazy.of(Optional.of(CurrentState.initial(state)));

            afterStateChange();

            log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.START));

            new Started(id).fire();

            state.getCurrentPlayers().stream()
                    .map(com.boardgamefiesta.api.domain.Player::getName)
                    .map(Player.Id::of)
                    .map(this::getPlayerById)
                    .flatMap(Optional::stream)
                    .forEach(player -> {
                        player.beginTurn(game.getTimeLimit(options));

                        new BeginTurn(game.getId(), id, type, player.getUserId(),
                                player.getTurnLimit().orElse(null),
                                started).fire();
                    });
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
        }
    }

    public Optional<Instant> getExpires() {
        switch (status) {
            case NEW:
                return Optional.of(created.plus(RETENTION_NEW));
            case ENDED:
                return Optional.of(ended.plus(RETENTION_AFTER_ENDED));
            case ABANDONED:
                return Optional.of(updated.plus(RETENTION_AFTER_ABANDONED));
            default:
                return Optional.empty();
        }
    }

    public void perform(@NonNull Player player, @NonNull Action action) {
        checkStarted();

        checkTurn(player);

        try {
            runStateChange(state -> state.perform(state.getPlayerByName(player.getId().getId())
                    .orElseThrow(NotPlayer::new), action, RANDOM));
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
        }
    }

    public void executeAutoma(Player player) {
        checkStarted();

        checkTurn(player);

        if (player.getType() != Player.Type.COMPUTER) {
            throw new IllegalStateException("Player is not computer");
        }

        runStateChange(state -> game.executeAutoma(state, getPlayer(player), RANDOM));
    }

    private void checkTurn(Player player) {
        if (!players.contains(player) || !player.isTurn()) {
            throw new NotPlayerTurn();
        }
    }

    private void runStateChange(Consumer<State> change) {
        var currentState = this.currentState.get().orElseThrow(NotStarted::new);
        var state = currentState.getState();

        var currentPlayers = state.getCurrentPlayers().stream()
                .map(com.boardgamefiesta.api.domain.Player::getName)
                .map(Player.Id::of)
                .map(this::getPlayerById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        EventListener eventListener = event -> log.add(new LogEntry(this, event));
        state.addEventListener(eventListener);
        try {
            change.accept(state);
        } finally {
            state.removeEventListener(eventListener);
        }

        currentState.next(state);

        // TODO Move this into afterStateChange to also support undoing after turn ends
        var newCurrentPlayers = state.getCurrentPlayers().stream()
                .map(com.boardgamefiesta.api.domain.Player::getName)
                .map(Player.Id::of)
                .map(this::getPlayerById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        currentPlayers.stream()
                .filter(player -> state.isEnded() || !newCurrentPlayers.contains(player))
                .forEach(Player::endTurn);

        if (!state.isEnded()) {
            newCurrentPlayers.stream()
                    .filter(newCurrentPlayer -> !currentPlayers.contains(newCurrentPlayer))
                    .forEach(player -> {
                        player.beginTurn(game.getTimeLimit(options));

                        new BeginTurn(game.getId(), id, type, player.getUserId(),
                                player.getTurnLimit().orElse(null),
                                started).fire();
                    });
        }

        afterStateChange();
    }

    public void skip(Player player) {
        checkStarted();

        checkTurn(player);

        try {
            runStateChange(state -> state.skip(getPlayer(player), RANDOM));
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
        }
    }

    public void endTurn(Player player) {
        checkStarted();

        try {
            runStateChange(state -> state.endTurn(getPlayer(player), RANDOM));
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
        }

        player.endTurn();
    }

    public void leave(@NonNull User.Id userId) {
        if (status == Status.ENDED) {
            throw new AlreadyEnded();
        }

        if (status == Status.ABANDONED) {
            throw new AlreadyAbandoned();
        }

        var player = getPlayerByUserId(userId)
                .orElseThrow(NotPlayer::new);

        if (ownerId.equals(userId)) {
            // if owner wants to leave, have to appoint a new owner
            otherUsersPlaying(userId)
                    .findAny()
                    .flatMap(Player::getUserId)
                    .ifPresentOrElse(this::changeOwner, this::abandon);
        }

        log.add(new LogEntry(player, LogEntry.Type.LEFT));

        player.leave();

        if (status == Status.NEW) {
            players.remove(player);
        }

        if (status == Status.STARTED) {
            if (players.stream().filter(Player::isPlaying).count() >= game.getMinNumberOfPlayers()) {
                // Game is still able to continue with one less player
                runStateChange(state -> state.leave(state.getPlayerByName(player.getId().getId()).orElseThrow(), RANDOM));
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
        checkStarted();

        var player = getPlayerByUserId(userId)
                .orElseThrow(NotPlayer::new);

        player.proposeToLeave();

        log.add(new LogEntry(player, LogEntry.Type.PROPOSED_TO_LEAVE));

        new ProposedToLeave(id, userId).fire();
    }

    public void agreeToLeave(@NonNull User.Id userId) {
        checkStarted();

        var player = getPlayerByUserId(userId)
                .orElseThrow(NotPlayer::new);

        player.agreeToLeave();

        new AgreedToLeave(id, userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.AGREED_TO_LEAVE));

        updated = Instant.now();

        if (otherUsersPlaying(userId).allMatch(Player::hasAgreedToLeave)) {
            abandon();
        }
    }

    private void checkStarted() {
        if (status != Status.STARTED) {
            throw new NotStarted();
        }
    }

    private void afterStateChange() {
        updated = Instant.now();

        new StateChanged(id, Optional.of(this)).fire();

        var state = getState();

        if (state.isEnded()) {
            status = Status.ENDED;
            ended = updated;

            var winners = state.winners();

            for (Player player : players) {
                state.getPlayerByName(player.getId().getId()).ifPresent(playerInState ->
                        state.score(playerInState).ifPresent(score ->
                                player.assignScore(score, winners.contains(playerInState))));
            }

            new Ended(id).fire();
        } else {
            for (Player player : players) {
                state.getPlayerByName(player.getId().getId())
                        .flatMap(state::score)
                        .ifPresent(score -> player.assignScore(score, false));
            }
        }
    }

    public void acceptInvite(@NonNull User.Id userId) {
        checkNew();

        var player = players.stream()
                .filter(p -> userId.equals(p.getUserId().orElse(null)))
                .findAny()
                .orElseThrow(NotPlayer::new);

        player.accept();
        new Accepted(id, userId).fire();

        log.add(new LogEntry(player, LogEntry.Type.ACCEPT));

        updated = Instant.now();
    }

    public void abandon() {
        if (status == Status.ENDED) {
            throw new AlreadyEnded();
        }

        if (status == Status.ABANDONED) {
            throw new AlreadyAbandoned();
        }

        if (otherUsersPlaying(ownerId).count() > 1) {
            throw new AbandonNotAllowed();
        }

        status = Status.ABANDONED;
        updated = Instant.now();

        new Abandoned(id).fire();
    }

    private Stream<Player> playersThatAccepted() {
        return players.stream().filter(player -> player.getStatus() == Player.Status.ACCEPTED);
    }

    public void rejectInvite(@NonNull User.Id userId) {
        checkNew();

        var player = players.stream()
                .filter(p -> userId.equals(p.getUserId().orElse(null)))
                .findAny()
                .orElseThrow(NotPlayer::new);

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

    public Set<Player> getCurrentPlayers() {
        return players.stream().filter(Player::isTurn).collect(Collectors.toSet());
    }

    public State getState() {
        return currentState.get().orElseThrow(NotStarted::new).getState();
    }

    public Optional<Player> getPlayerById(Player.Id playerId) {
        return players.stream()
                .filter(player -> playerId.equals(player.getId()))
                .findAny();
    }

    public void invite(User user) {
        checkNew();

        if (players.size() == game.getMaxNumberOfPlayers()) {
            throw new ExceedsMaxPlayers();
        }

        if (isPlayer(user.getId())) {
            throw new AlreadyInvited();
        }

        var player = Player.invite(user.getId());
        players.add(player);

        log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.INVITE, List.of(user.getId().getId())));

        new Invited(id, user.getId(), game.getId(), ownerId).fire();
    }

    public void kick(Player player) {
        checkNew();

        // TODO With enough time passed, or votes, a player can be removed after the game has started

        if (!players.remove(player)) {
            throw new NotPlayer();
        }

        if (player.getType() == Player.Type.USER) {
            var userId = player.getUserId().orElseThrow();

            log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.KICK, List.of(userId.getId())));

            new Kicked(this.id, userId).fire();
        }
    }

    public void join(@NonNull User.Id userId) {
        checkNew();

        if (visibility != Visibility.PUBLIC) {
            throw new NotPublic();
        }

        if (players.size() == game.getMaxNumberOfPlayers()) {
            throw new ExceedsMaxPlayers();
        }

        if (isPlayer(userId)) {
            throw new AlreadyInvited();
        }

        var player = Player.accepted(userId);
        players.add(player);

        log.add(new LogEntry(player, LogEntry.Type.JOIN, List.of(userId)));

        new Joined(id, userId).fire();
    }

    private boolean isPlayer(User.@NonNull Id userId) {
        return players.stream().anyMatch(player -> userId.equals(player.getUserId().orElse(null)));
    }

    public void makePublic() {
        checkNew();

        visibility = Visibility.PUBLIC;

        new VisibilityChanged(id).fire();
    }

    public void makePrivate() {
        checkNew();

        visibility = Visibility.PRIVATE;

        new VisibilityChanged(id).fire();
    }

    public void addComputer() {
        checkNew();

        if (players.size() == game.getMaxNumberOfPlayers()) {
            throw new ExceedsMaxPlayers();
        }

        if (!game.hasAutoma()) {
            throw new ComputerNotSupported();
        }

        players.add(Player.computer());

        new ComputerAdded(id);
    }

    public void changeOptions(@NonNull Options options) {
        checkNew();

        this.options = options;

        new OptionsChanged(id).fire();
    }

    private void checkNew() {
        if (status == Status.STARTED) {
            throw new AlreadyStarted();
        }
        if (status == Status.ENDED) {
            throw new AlreadyEnded();
        }
        if (status == Status.ABANDONED) {
            throw new AlreadyAbandoned();
        }
    }

    public void undo(Player player) {
        checkStarted();

        var currentState = this.currentState.get()
                .orElseThrow(NotStarted::new);

        checkTurn(player);

        if (!currentState.canUndo() || currentState.getState().getCurrentPlayers().size() > 1) {
            throw new UndoNotAllowed();
        }

        var previous = currentState.getPrevious()
                .get()
                .orElseThrow(HistoryNotAvailable::new);

        log.add(new LogEntry(player, LogEntry.Type.UNDO));

        currentState.revertTo(previous);

        afterStateChange();
    }

    private com.boardgamefiesta.api.domain.Player getPlayer(Player player) {
        var state = currentState.get().orElseThrow(NotStarted::new).getState();
        return state.getPlayerByName(player.getId().getId())
                .orElseThrow(NotPlayer::new);
    }

    public boolean canUndo() {
        return currentState.get().map(CurrentState::getState)
                .map(state -> state.canUndo() && state.getCurrentPlayers().size() == 1)
                .orElse(false);
    }

    public boolean isActive() {
        return status != Status.ENDED && status != Status.ABANDONED;
    }

    public boolean hasComputerPlayers() {
        return players.stream().anyMatch(player -> player.getType() == Player.Type.COMPUTER);
    }

    public boolean canJoin(User.Id userId) {
        return status == Status.NEW && visibility == Visibility.PUBLIC
                && !isPlayer(userId) && players.size() < game.getMaxNumberOfPlayers();
    }

    public void changeType(Type type) {
        checkNew();

        this.type = type;

        new OptionsChanged(id).fire();
    }

    public Map<User.Id, Integer> getUserScores() {
        return players.stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .collect(Collectors.toMap(
                        player -> player.getUserId().orElseThrow(),
                        player -> player.getScore().orElseThrow()));
    }

    public boolean canLeave(User.Id userId) {
        return (status == Status.NEW || status == Status.STARTED)
                && getPlayerByUserId(userId)
                .map(Player::isPlaying)
                .orElse(false);
    }

    public boolean canJoin() {
        return status == Status.NEW
                && visibility == Visibility.PUBLIC
                && players.size() < game.getMaxNumberOfPlayers();
    }

    public Optional<Stats> stats(Player player) {
        var state = getState();

        return state.getPlayerByName(player.getId().getId()) // could be empty when player has left
                .map(state::stats);
    }

    public enum Status {
        NEW,
        STARTED,
        ABANDONED,
        ENDED
    }

    public enum Type {
        REALTIME,
        TURN_BASED
    }

    public enum Mode {
        NORMAL,
        TRAINING
    }

    public enum Visibility {
        PUBLIC,
        PRIVATE
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
        Game.Id gameId;
        User.Id hostId;
    }

    @Value
    public static class Joined implements DomainEvent {
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
        Optional<Table> table;
    }

    @Value
    public static class VisibilityChanged implements DomainEvent {
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

    @Value
    public static class BeginTurn implements DomainEvent {
        @NonNull Game.Id gameId;
        @NonNull Table.Id tableId;
        @NonNull Table.Type type;
        @NonNull Optional<User.Id> userId;
        @NonNull Instant limit;
        @NonNull Instant started;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    @Getter
    public static class CurrentState {
        private State state;
        private Instant timestamp;
        private Lazy<Optional<HistoricState>> previous;
        private boolean changed;

        public static CurrentState initial(State state) {
            return new CurrentState(state, Instant.now(), Lazy.of(Optional.empty()), true);
        }

        public HistoricState next(State state) {
            var previous = HistoricState.from(this);

            this.state = state;
            this.timestamp = Instant.now();
            this.previous = Lazy.of(Optional.of(previous));
            this.changed = true;

            return previous;
        }

        public void revertTo(HistoricState historicState) {
            this.state = historicState.getState();
            this.previous = historicState.getPrevious();
            this.timestamp = Instant.now();
            this.changed = true;
        }

        public boolean canUndo() {
            return state.canUndo() && previous.get().isPresent();
        }

        public boolean isChanged() {
            return changed;
        }

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    @Getter
    public static class HistoricState {
        protected State state;
        protected Instant timestamp;
        protected Lazy<Optional<HistoricState>> previous;

        public static HistoricState from(CurrentState currentState) {
            return new HistoricState(currentState.state, currentState.timestamp, currentState.previous);
        }
    }

    public static final class InGameError extends InvalidCommandException {

        @Getter
        private final Game.Id gameId;

        private InGameError(Game.Id gameId, InGameException cause) {
            super("IN_GAME_ERROR", cause);
            this.gameId = gameId;
        }

    }

    public static final class NotPublic extends NotAllowedException {
        private NotPublic() {
            super("NOT_PUBLIC");
        }
    }

    public static final class NotEnoughPlayers extends InvalidCommandException {
        private NotEnoughPlayers() {
            super("MIN_PLAYERS");
        }
    }

    public static final class ExceedsMaxPlayers extends InvalidCommandException {
        private ExceedsMaxPlayers() {
            super("EXCEEDS_MAX_PLAYERS");
        }
    }

    public static final class ComputerNotSupported extends InvalidCommandException {
        private ComputerNotSupported() {
            super("COMPUTER_NOT_SUPPORTED");
        }
    }

    public static final class HistoryNotAvailable extends InsufficientDataException {
        private HistoryNotAvailable() {
            super("HISTORY_NOT_AVAILABLE");
        }
    }

    public static final class NotPlayer extends NotAllowedException {
        private NotPlayer() {
            super("NOT_PLAYER_IN_GAME");
        }
    }

    public static final class NotPlayerTurn extends NotAllowedException {
        private NotPlayerTurn() {
            super("NOT_YOUR_TURN");
        }
    }

    public static final class AlreadyEnded extends NotAllowedException {
        private AlreadyEnded() {
            super("GAME_ALREADY_ENDED");
        }
    }

    public static final class AlreadyAbandoned extends NotAllowedException {
        private AlreadyAbandoned() {
            super("ALREADY_ABANDONED");
        }
    }

    public static final class AlreadyStarted extends NotAllowedException {
        private AlreadyStarted() {
            super("GAME_ALREADY_STARTED");
        }
    }

    public static final class NotStarted extends NotAllowedException {
        private NotStarted() {
            super("GAME_NOT_STARTED");
        }
    }

    public static final class AlreadyInvited extends NotAllowedException {
        private AlreadyInvited() {
            super("ALREADY_INVITED");
        }
    }

    public static final class AbandonNotAllowed extends NotAllowedException {
        private AbandonNotAllowed() {
            super("CANNOT_ABANDON");
        }
    }

    public static final class UndoNotAllowed extends NotAllowedException {
        private UndoNotAllowed() {
            super("CANNOT_UNDO");
        }
    }

}
