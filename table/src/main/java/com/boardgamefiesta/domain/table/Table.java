/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.table;

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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(doNotUseGetters = true)
public class Table implements AggregateRoot {

    private static final Duration RETENTION_NEW = Duration.of(2, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ENDED = Duration.of(365 * 5, ChronoUnit.DAYS);
    private static final Duration RETENTION_AFTER_ABANDONED = Duration.of(1, ChronoUnit.DAYS);

    private static final Duration REALTIME_TURN_LIMIT = Duration.of(10, ChronoUnit.MINUTES);
    private static final Duration TURN_BASED_TURN_LIMIT = Duration.of(12, ChronoUnit.HOURS);

    private static final int MAX_PROGRESS_TO_ABANDON = 10;
    private static final int MIN_PROGRESS_TO_KEEP_WHEN_ABANDONED = 25;

    static Random RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstance("NativePRNGNonBlocking");
        } catch (NoSuchAlgorithmException e1) {
            try {
                RANDOM = SecureRandom.getInstance("Windows-PRNG");
            } catch (NoSuchAlgorithmException e2) {
                throw new IllegalStateException("Could not initialize PRNG", e2);
            }
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
    private final List<Seat> seats;

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
    private int progress;

    @Getter
    @NonNull
    private User.Id ownerId;

    @Getter
    private Lazy<Optional<CurrentState>> currentState;

    @Getter
    private HistoricStates historicStates;

    @Getter
    private Instant updated;

    @Getter
    private Instant started;

    @Getter
    private Instant ended;

    @Getter
    private int minNumberOfPlayers;

    @Getter
    private int maxNumberOfPlayers;

    @Getter
    private boolean autoStart;

    public static Table create(@NonNull Game game,
                               @NonNull Type type,
                               @NonNull Mode mode,
                               @NonNull User owner,
                               @NonNull Options options) {
        var seats = IntStream.range(0, game.getMaxNumberOfPlayers())
                .mapToObj(index -> Seat.empty())
                .collect(Collectors.toCollection(ArrayList::new));

        var player = seats.get(0).assign(owner, game.getSupportedColors());

        var created = Instant.now();

        Table table = Table.builder()
                .id(Id.generate())
                .version(1)
                .game(game)
                .type(type)
                .mode(mode)
                .visibility(Visibility.PRIVATE)
                .status(Status.NEW)
                .options(options)
                .created(created)
                .updated(created)
                .ownerId(owner.getId())
                .seats(seats)
                .players(new HashSet<>(Collections.singleton(player)))
                .log(new Log())
                .currentState(Lazy.of(Optional.empty()))
                .historicStates(new HistoricStates())
                .minNumberOfPlayers(game.getMinNumberOfPlayers())
                .maxNumberOfPlayers(game.getMaxNumberOfPlayers())
                .autoStart(false)
                .build();

        table.log.add(new LogEntry(player, LogEntry.Type.CREATE));

        new Created(Lazy.of(table), table.getId()).fire();

        return table;
    }

    public void start() {
        checkNew();

        if (!hasCurrentState()) {
            players.removeIf(player -> player.getStatus() != Player.Status.ACCEPTED);
            seats.removeIf(seat -> seat.getPlayer().isEmpty() || seat.getPlayer().get().getStatus() != Player.Status.ACCEPTED);
        }

        if (players.size() < game.getMinNumberOfPlayers()) {
            throw new NotEnoughPlayers();
        }

        var randomColors = new ArrayList<>(getAvailableColors());
        Collections.shuffle(randomColors, RANDOM);

        players.forEach(player -> {
            if (player.getColor().isEmpty()) {
                player.assignColor(randomColors.remove(randomColors.size() - 1));
            }
        });

        status = Status.STARTED;
        started = Instant.now();
        updated = started;

        try {
            log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.START));

            InGameEventListener eventListener = event -> log.add(new LogEntry(this, event));

            if (!hasCurrentState()) {
                var state = game.start(players.stream()
                        .map(player -> new com.boardgamefiesta.api.domain.Player(player.getId().getId(), player.getColor().orElseThrow(),
                                player.getType() == Player.Type.COMPUTER
                                        ? com.boardgamefiesta.api.domain.Player.Type.COMPUTER
                                        : com.boardgamefiesta.api.domain.Player.Type.HUMAN))
                        .collect(Collectors.toSet()), options, eventListener, RANDOM);

                currentState = Lazy.of(Optional.of(CurrentState.initial(state)));
            }

            new Started(Lazy.of(this), id).fire();

            afterStateChange();
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
        }
    }

    private boolean hasCurrentState() {
        return currentState.get().isPresent();
    }

    private void beginTurn(Player player) {
        player.beginTurn(type == Type.TURN_BASED ? TURN_BASED_TURN_LIMIT : REALTIME_TURN_LIMIT);

        log.add(new LogEntry(player, LogEntry.Type.BEGIN_TURN));

        new BeginTurn(Lazy.of(this), game.getId(), id, type, player.getUserId(), player.getTurnLimit().get(), started).fire();
    }

    private void endTurnInternal(Player player) {
        player.endTurn();

        if (player.getStatus() != Player.Status.LEFT) {
            log.add(new LogEntry(player, LogEntry.Type.END_TURN));

            new EndTurn(Lazy.of(this), id, player.getUserId(), Instant.now()).fire();
        }
    }

    public Optional<Instant> getExpires() {
        switch (status) {
            case NEW:
                return Optional.of(created.plus(RETENTION_NEW));
            case ENDED:
                return Optional.of(ended.plus(RETENTION_AFTER_ENDED));
            case ABANDONED:
                return Optional.of(progress >= MIN_PROGRESS_TO_KEEP_WHEN_ABANDONED
                        ? updated.plus(RETENTION_AFTER_ENDED)
                        : updated.plus(RETENTION_AFTER_ABANDONED));
            default:
                return Optional.empty();
        }
    }

    public void perform(@NonNull Player player, @NonNull Action action) {
        checkStarted();

        checkTurn(player);

        runStateChange(state -> state.perform(player.asPlayer(), action, RANDOM));
    }

    public void executeAutoma(Player player) {
        checkStarted();

        checkTurn(player);

        if (player.getType() != Player.Type.COMPUTER) {
            throw new IllegalStateException("Player is not computer");
        }

        runStateChange(state -> game.executeAutoma(state, player.asPlayer(), RANDOM));
    }

    private void checkTurn(Player player) {
        if (!players.contains(player) || !player.isTurn()) {
            throw new NotPlayerTurn();
        }
    }

    private void runStateChange(Consumer<State> change) {
        var currentState = this.currentState.get().orElseThrow(NotStarted::new);
        var state = currentState.getState();

        var historicState = HistoricState.from(currentState);

        InGameEventListener eventListener = event -> log.add(new LogEntry(this, event));
        state.addEventListener(eventListener);
        try {
            change.accept(state);
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
        } finally {
            state.removeEventListener(eventListener);
        }

        currentState.next(state, historicState);
        historicStates.add(historicState);

        afterStateChange();
    }

    public void skip(Player player) {
        checkStarted();

        checkTurn(player);

        log.add(new LogEntry(player, LogEntry.Type.SKIP));

        runStateChange(state -> state.skip(player.asPlayer(), RANDOM));
    }

    public void endTurn(Player player) {
        checkStarted();

        runStateChange(state -> state.endTurn(player.asPlayer(), RANDOM));
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

        var seat = getSeatByPlayer(player)
                .orElseThrow(NotPlayer::new);

        log.add(new LogEntry(player, LogEntry.Type.LEFT));

        player.leave();

        if (status == Status.NEW) {
            players.remove(player);
            seat.unassign();
            shrinkSeatsIfNeeded();
        }

        new Left(Lazy.of(this), id, userId, Instant.now()).fire();

        updated = Instant.now();

        afterPlayerLeft(player);
    }

    void afterPlayerLeft(Player tablePlayer) {
        tablePlayer.getUserId().ifPresent(userId -> {
            if (ownerId.equals(userId)) {
                // if owner wants to leave, have to appoint a new owner
                otherUsersPlaying(userId)
                        .findAny()
                        .flatMap(Player::getUserId)
                        .ifPresentOrElse(this::changeOwner, this::abandon);
            }
        });

        if (status == Status.STARTED) {
            var player = tablePlayer.asPlayer();

            runStateChange(state -> state.leave(player, RANDOM));

            if (status != Status.ENDED /* not automatically ended by state change */
                    && players.stream().filter(Player::isPlaying).count() < game.getMinNumberOfPlayers()) {
                // Game cannot continue with one less player

                // If the game is only against computer players, or if the game has not progressed enough yet, then just abandon
                if (!hasMoreThanOneHumanPlayer() || progress > MAX_PROGRESS_TO_ABANDON) {
                    abandon();
                } else {
                    end();
                }
            }
        }
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

        new ChangedOwner(Lazy.of(this), id, userId).fire();
    }

    private void checkStarted() {
        if (status != Status.STARTED) {
            throw new NotStarted();
        }
    }

    private void afterStateChange() {
        final State state = getState();

        if (status == Status.STARTED) {
            var newCurrentPlayers = state.getCurrentPlayers().stream()
                    .map(com.boardgamefiesta.api.domain.Player::getName)
                    .map(Player.Id::of)
                    .map(this::getPlayerById)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());

            players.forEach(player -> {
                if (state.isEnded() || !newCurrentPlayers.contains(player)) {
                    if (player.isTurn()) {
                        endTurnInternal(player);
                    }
                } else {
                    if (!player.isTurn()) {
                        beginTurn(player);
                    }
                }
            });
        }

        updated = Instant.now();

        new StateChanged(id, Lazy.of(this)).fire();

        if (state.isEnded()) {
            progress = 100;
            end();
        } else {
            progress = state.getProgress();
            assignScores();
        }
    }

    private void end() {
        status = Status.ENDED;
        ended = Instant.now();

        assignScores();

        log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.END));

        new Ended(Lazy.of(this), id, ended).fire();
    }

    private void assignScores() {
        var state = getState();

        var winner = status == Status.ENDED ? state.getRanking().get(0) : null;

        for (Player player : players) {
            if (player.isPlaying()) {
                var score = state.getScore(player.asPlayer());
                player.assignScore(score, winner != null && winner.getName().equals(player.getId().getId()));
            } else {
                // Player has left during the game, always score 0
                player.assignScore(0, false);
            }
        }
    }

    public void acceptInvite(@NonNull User user) {
        checkNew();

        var player = players.stream()
                .filter(p -> user.getId().equals(p.getUserId().orElse(null)))
                .findAny()
                .orElseThrow(NotPlayer::new);

        player.accept(user, getAvailableColors());
        new Accepted(Lazy.of(this), id, user.getId()).fire();

        log.add(new LogEntry(player, LogEntry.Type.ACCEPT));

        updated = Instant.now();

        autoStartIfPossible();
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

        new Abandoned(Lazy.of(this), id).fire();
    }

    private Stream<Player> playersThatAccepted() {
        return players.stream().filter(player -> player.getStatus() == Player.Status.ACCEPTED);
    }

    public void rejectInvite(@NonNull User.Id userId) {
        checkNew();

        var player = getPlayerByUserId(userId)
                .orElseThrow(NotPlayer::new);

        var seat = getSeatByPlayer(player)
                .orElseThrow(NotPlayer::new);

        seat.unassign();
        shrinkSeatsIfNeeded();
        players.remove(player);

        new Rejected(Lazy.of(this), id, userId).fire();

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

    public Set<PlayerColor> getAvailableColors() {
        var colors = new HashSet<>(game.getSupportedColors());

        players.forEach(player -> player.getColor().ifPresent(colors::remove));

        return Collections.unmodifiableSet(colors);
    }

    public Player invite(User user) {
        checkNew();

        if (players.size() == maxNumberOfPlayers) {
            throw new ExceedsMaxPlayers();
        }

        if (isPlayer(user.getId())) {
            throw new AlreadyInvited();
        }

        var seat = seats.stream()
                .filter(Seat::isAvailable)
                .findAny()
                .orElseThrow(SeatNotAvailable::new);

        var player = seat.invite(user);
        players.add(player);

        var owner = getPlayerByUserId(ownerId).orElseThrow();
        log.add(new LogEntry(owner, LogEntry.Type.INVITE, List.of(user.getId().getId())));

        new Invited(Lazy.of(this), id, type, user.getId(), game.getId(), ownerId).fire();

        return player;
    }

    public void kick(@NonNull User.Id currentUserId, @NonNull Player player) {
        checkPlayer(player);

        var kickingPlayer = getPlayerByUserId(currentUserId)
                .filter(Player::isActive)
                .orElseThrow(NotPlayer::new);

        var seat = getSeatByPlayer(player)
                .orElseThrow(NotPlayer::new);

        if (status == Status.NEW) {
            checkOwner(currentUserId);

            players.remove(player);
        } else {
            checkStarted();

            player.kick();
        }

        seat.unassign();
        shrinkSeatsIfNeeded();

        player.getUserId().ifPresent(userId -> {
            log.add(new LogEntry(kickingPlayer, LogEntry.Type.KICK, List.of(userId.getId())));

            new Kicked(Lazy.of(this), this.id, userId, Instant.now()).fire();
        });

        afterPlayerLeft(player);
    }

    private Optional<Seat> getSeatByPlayer(Player player) {
        return seats.stream()
                .filter(s -> s.getPlayer().orElse(null) == player)
                .findAny();
    }

    public void forceEndTurn(@NonNull User.Id currentUserId, @NonNull Player player) {
        checkStarted();
        checkPlayer(player);

        var forcingPlayer = getPlayerByUserId(currentUserId)
                .filter(Player::isActive)
                .orElseThrow(NotPlayer::new);

        var userId = player.getUserId().orElseThrow();

        player.forceEndTurn();

        log.add(new LogEntry(forcingPlayer, LogEntry.Type.FORCE_END_TURN, List.of(userId.getId())));

        runStateChange(state -> state.forceEndTurn(player.asPlayer(), RANDOM));

        new ForcedEndTurn(Lazy.of(this), id, userId, Instant.now()).fire();
    }

    private void checkPlayer(Player player) {
        if (!players.contains(player)) {
            throw new NotPlayer();
        }
    }

    private void checkOwner(User.Id userId) {
        if (!ownerId.equals(userId)) {
            throw new MustBeOwner();
        }
    }

    public Player join(@NonNull User user) {
        checkNew();

        if (visibility != Visibility.PUBLIC) {
            throw new NotPublic();
        }

        if (players.size() == maxNumberOfPlayers) {
            throw new ExceedsMaxPlayers();
        }

        if (isPlayer(user.getId())) {
            throw new AlreadyInvited();
        }

        var seat = seats.stream()
                .filter(Seat::isAvailable)
                .findAny()
                .orElseThrow(SeatNotAvailable::new);

        var player = seat.assign(user, getAvailableColors());
        players.add(player);

        log.add(new LogEntry(player, LogEntry.Type.JOIN, List.of(user.getId())));

        new Joined(Lazy.of(this), id, user.getId()).fire();

        autoStartIfPossible();

        return player;
    }

    private void autoStartIfPossible() {
        if (autoStart && playersThatAccepted().count() >= minNumberOfPlayers) {
            start();
        }
    }

    private boolean isPlayer(User.@NonNull Id userId) {
        return players.stream().anyMatch(player -> userId.equals(player.getUserId().orElse(null)));
    }

    public void makePublic() {
        checkNew();

        visibility = Visibility.PUBLIC;

        new VisibilityChanged(Lazy.of(this), id).fire();
    }

    public void makePrivate() {
        checkNew();

        visibility = Visibility.PRIVATE;

        new VisibilityChanged(Lazy.of(this), id).fire();
    }

    public Player addComputer() {
        checkNew();

        if (players.size() == maxNumberOfPlayers) {
            throw new ExceedsMaxPlayers();
        }

        if (!game.hasAutoma()) {
            throw new ComputerNotSupported();
        }

        var seat = seats.stream()
                .filter(Seat::isAvailable)
                .findAny()
                .orElseThrow(SeatNotAvailable::new);

        var player = seat.computer();
        players.add(player);

        new ComputerAdded(Lazy.of(this), id);

        return player;
    }

    public void changeOptions(@NonNull Options options) {
        checkNew();
        checkNoState();

        this.options = options;

        new OptionsChanged(Lazy.of(this), id).fire();
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

    public boolean hasMoreThanOneHumanPlayer() {
        return players.stream().filter(player -> player.getType() == Player.Type.USER).count() > 1;
    }

    public boolean canJoin(User.Id userId) {
        return status == Status.NEW && visibility == Visibility.PUBLIC
                && !isPlayer(userId) && players.size() < maxNumberOfPlayers;
    }

    public void changeType(Type type) {
        checkNew();

        this.type = type;

        new OptionsChanged(Lazy.of(this), id).fire();
    }

    public void changeColor(@NonNull Player player, PlayerColor color) {
        checkPlayer(player);

        player.getColor()
                .filter(currentColor -> currentColor != color)
                .ifPresent(currentDifferentColor -> {
                    if (color != null) {
                        var availableColors = getAvailableColors();

                        if (!availableColors.contains(color)) {
                            throw new ColorNotAvailable();
                        }
                    }

                    player.assignColor(color);
                });
    }

    public void changeSeat(@NonNull Player player, int seat) {
        var currentSeat = getSeatByPlayer(player)
                .orElseThrow(NotPlayer::new);

        if (seat < 0 || seat >= seats.size()) {
            throw new SeatNotAvailable();
        }

        var targetSeat = seats.get(seat);

        if (targetSeat != currentSeat) {
            players.remove(currentSeat.unassign().get());

            targetSeat.unassign().ifPresent(otherPlayer -> {
                players.remove(otherPlayer);
                players.add(currentSeat.assign(otherPlayer));
            });

            players.add(targetSeat.assign(player));
        }
    }

    public List<User.Id> getUserRanking() {
        return currentState.get()
                .map(CurrentState::getState)
                .map(State::getRanking)
                .map(ranking -> ranking.stream()
                        .map(com.boardgamefiesta.api.domain.Player::getName)
                        .map(Player.Id::of)
                        .flatMap(playerId -> getPlayerById(playerId).stream())
                        .flatMap(player -> player.getUserId().stream())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
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
                && players.size() < maxNumberOfPlayers;
    }

    public Optional<Stats> stats(Player player) {
        var state = getState();

        return Optional.of(state.getStats(player.asPlayer()));
    }

    public void changeMode(@NonNull Mode mode) {
        checkNew();

        this.mode = mode;

        new OptionsChanged(Lazy.of(this), id).fire();
    }

    public void changeMinMaxNumberOfPlayers(int minNumberOfPlayers, int maxNumberOfPlayers) {
        checkNew();
        checkNoState();

        if (minNumberOfPlayers < game.getMinNumberOfPlayers() || minNumberOfPlayers > game.getMaxNumberOfPlayers()) {
            throw new MinNumberOfPlayers();
        }
        if (maxNumberOfPlayers > game.getMaxNumberOfPlayers() || maxNumberOfPlayers < game.getMinNumberOfPlayers()) {
            throw new MaxNumberOfPlayers();
        }
        if (maxNumberOfPlayers < minNumberOfPlayers) {
            throw new MaxNumberOfPlayers();
        }

        this.minNumberOfPlayers = minNumberOfPlayers;
        this.maxNumberOfPlayers = maxNumberOfPlayers;

        shrinkSeatsIfNeeded();
        while (seats.size() < maxNumberOfPlayers) {
            seats.add(Seat.empty());
        }

        new OptionsChanged(Lazy.of(this), id).fire();
    }

    private void shrinkSeatsIfNeeded() {
        for (int n = seats.size() - maxNumberOfPlayers; n > 0; n--) {
            seats.removeIf(Seat::isAvailable);
        }
    }

    private void checkNoState() {
        if (hasCurrentState()) {
            throw new StateNotCompatible();
        }
    }

    public void changeAutoStart(boolean autoStart) {
        checkNew();

        this.autoStart = autoStart;

        new OptionsChanged(Lazy.of(this), id).fire();
    }

    public void revertTo(@NonNull final HistoricState historicState) {
        checkStarted();

        if (mode != Mode.PRACTICE) {
            throw new NotPracticeMode();
        }

        var currentState = this.currentState.get().orElseThrow(NotStarted::new);

        currentState.revertTo(historicState);

        afterStateChange();
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
        PRACTICE
    }

    public enum Visibility {
        PUBLIC,
        PRIVATE
    }

    @Value(staticConstructor = "fromString")
    public static class Id {
        String id;

        private static Id generate() {
            return of(UUID.randomUUID().toString());
        }

        /**
         * @deprecated For backwards compatibility. Use {@link #fromString(String)} instead.
         */
        @Deprecated
        public static Table.Id of(String str) {
            return fromString(str);
        }
    }

    @Value
    public static class Invited implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull Table.Type type;
        @NonNull User.Id userId;
        @NonNull Game.Id gameId;
        @NonNull User.Id hostId;
    }

    @Value
    public static class Joined implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class Kicked implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
        @NonNull Instant timestamp;
    }

    @Value
    public static class ForcedEndTurn implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
        @NonNull Instant timestamp;
    }

    @Value
    public static class Accepted implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class Rejected implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class Started implements DomainEvent {
        @NonNull Lazy<Table> table;
        Table.Id tableId;
    }

    @Value
    public static class Ended implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull Instant timestamp;
    }

    @Value
    public static class StateChanged implements DomainEvent {
        @NonNull Table.Id tableId;
        @NonNull Lazy<Table> table;
    }

    @Value
    public static class VisibilityChanged implements DomainEvent {
        @NonNull Lazy<Table> table;
        Table.Id tableId;
    }

    @Value
    public static class Created implements DomainEvent {
        @NonNull Lazy<Table> table;
        Table.Id tableId;
    }

    @Value
    private static class ChangedOwner implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class Left implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
        @NonNull Instant timestamp;
    }

    @Value
    public static class ProposedToLeave implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class AgreedToLeave implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
    }

    @Value
    public static class Abandoned implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
    }

    @Value
    public static class ComputerAdded implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
    }

    @Value
    public static class OptionsChanged implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
    }

    @Value
    public static class BeginTurn implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Game.Id gameId;
        @NonNull Table.Id tableId;
        @NonNull Table.Type type;
        @NonNull Optional<User.Id> userId;
        @NonNull Instant limit;
        @NonNull Instant started;
    }

    @Value
    public static class EndTurn implements DomainEvent {
        @NonNull Lazy<Table> table;
        @NonNull Table.Id tableId;
        @NonNull Optional<User.Id> userId;
        @NonNull Instant timestamp;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    @Getter
    public static class CurrentState {
        private State state;
        private Instant timestamp;
        private Lazy<Optional<HistoricState>> previous;
        private boolean changed; // TODO Better name

        public static CurrentState initial(State state) {
            return new CurrentState(state, Instant.now(), Lazy.of(Optional.empty()), true);
        }

        public void next(State state, HistoricState previous) {
            this.state = state;
            this.timestamp = Instant.now();
            this.previous = Lazy.of(Optional.of(previous));
            this.changed = true;
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
            // TODO Clone state to prevent it from being modified after this (and wrap in immutable?)
            return new HistoricState(currentState.state, currentState.timestamp, currentState.previous);
        }
    }

    /**
     * Forks (creates) a new table from a historic state of this table.
     *
     * <p>The number of players MUST be the same as this table and cannot be changed.</p>
     * <p>The options MUST be the same as this table and cannot be changed.</p>
     */
    public Table fork(final Instant timestamp, final Type type, final Mode mode, final User owner) {
        var historicState = historicStates.at(timestamp)
                .orElseThrow(HistoryNotAvailable::new);

        var state = historicState.getState();
        if (state.isEnded()) {
            throw new AlreadyEnded();
        }

        var numberOfPlayers = players.size();

        // All players must somehow be mapped, so create them all as seats
        var seats = players.stream()
                .map(Seat::fork)
                .collect(Collectors.toCollection(ArrayList::new));

        var ownerPlayer = getPlayerByUserId(owner.getId())
                .map(Player::getId)
                .flatMap(playerId -> seats.stream()
                        .filter(seat -> seat.getPlayerId().equals(playerId)) // Assign owner same seat as in original table
                        .findAny())
                .orElseGet(() -> seats.get(0)) // Assign any seat to owner
                .assign(owner, game.getSupportedColors());

        var created = Instant.now();

        var newTable = Table.builder()
                .id(Id.generate())
                .version(1)
                .game(game)
                .type(type)
                .mode(mode)
                .visibility(Visibility.PRIVATE)
                .status(Status.NEW)
                .options(options)
                .created(created)
                .updated(created)
                .ownerId(owner.getId())
                .seats(seats)
                .players(new HashSet<>(Collections.singleton(ownerPlayer)))
                .log(new Log()) // TODO Add in game events of historic state to log?
                .currentState(Lazy.of(Optional.of(CurrentState.initial(state))))
                .historicStates(new HistoricStates())
                .minNumberOfPlayers(numberOfPlayers)
                .maxNumberOfPlayers(numberOfPlayers)
                .autoStart(false)
                .build();

        newTable.log.add(new LogEntry(ownerPlayer, LogEntry.Type.CREATE));

        new Created(Lazy.of(newTable), newTable.getId()).fire();

        newTable.afterStateChange();

        return newTable;
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

    private class SeatNotAvailable extends InvalidCommandException {
        private SeatNotAvailable() {
            super("SEAT_NOT_AVAILABLE");
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

    public static final class StateNotCompatible extends NotAllowedException {
        private StateNotCompatible() {
            super("STATE_NOT_COMPATIBLE");
        }
    }

    public static final class NotPlayer extends NotAllowedException {
        private NotPlayer() {
            super("NOT_PLAYER_IN_GAME");
        }
    }

    public static final class MustBeOwner extends NotAllowedException {
        private MustBeOwner() {
            super("MUST_BE_OWNER");
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

    public static final class NotPracticeMode extends NotAllowedException {
        private NotPracticeMode() {
            super("NOT_PRACTICE_MODE");
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

    public static final class MinNumberOfPlayers extends NotAllowedException {
        private MinNumberOfPlayers() {
            super("MIN_NUMBER_OF_PLAYERS");
        }
    }

    public static final class MaxNumberOfPlayers extends NotAllowedException {
        private MaxNumberOfPlayers() {
            super("MAX_NUMBER_OF_PLAYERS");
        }
    }

    public static final class ColorNotAvailable extends InvalidCommandException {
        private ColorNotAvailable() {
            super("COLOR_NOT_AVAILABLE");
        }
    }

    public static class HistoricStates {

        private final SortedMap<Instant, HistoricState> historicStates = new TreeMap<>();

        protected final void add(HistoricState historicState) {
            if (historicStates.containsKey(historicState.timestamp)) {
                throw new IllegalStateException("timestamp already exists: " + historicState.timestamp);
            }
            historicStates.put(historicState.timestamp, historicState);
        }

        public Optional<HistoricState> at(Instant timestamp) {
            var head = historicStates.headMap(timestamp.plusNanos(1));

            if (head.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(head.get(head.lastKey()));
        }

        public Set<Instant> getTimestamps() {
            return historicStates.keySet();
        }

        public int count() {
            return historicStates.size();
        }

        public Optional<HistoricState> getLast() {
            return historicStates.isEmpty() ? Optional.empty() : Optional.of(historicStates.get(historicStates.lastKey()));
        }
    }

}
