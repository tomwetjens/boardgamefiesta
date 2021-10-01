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

    private static final Duration REALTIME_TURN_LIMIT = Duration.of(10, ChronoUnit.MINUTES);
    private static final Duration TURN_BASED_TURN_LIMIT = Duration.of(12, ChronoUnit.HOURS);

    private static final int MAX_TURNS_TO_ABANDON = 2;

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
        var player = Player.accepted(owner.getId());

        owner.getColorPreferences().pickPreferredColor(game.getSupportedColors())
                .ifPresent(player::assignColor);

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
                .players(new HashSet<>(Collections.singleton(player)))
                .log(new Log())
                .currentState(Lazy.of(Optional.empty()))
                .minNumberOfPlayers(game.getMinNumberOfPlayers())
                .maxNumberOfPlayers(game.getMaxNumberOfPlayers())
                .autoStart(false)
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

            EventListener eventListener = event -> log.add(new LogEntry(this, event));

            var state = game.start(players.stream()
                    .map(player -> new com.boardgamefiesta.api.domain.Player(player.getId().getId(), player.getColor().orElseThrow(),
                            player.getType() == Player.Type.COMPUTER
                                    ? com.boardgamefiesta.api.domain.Player.Type.COMPUTER
                                    : com.boardgamefiesta.api.domain.Player.Type.HUMAN))
                    .collect(Collectors.toSet()), options, eventListener, RANDOM);

            currentState = Lazy.of(Optional.of(CurrentState.initial(state)));

            afterStateChange();

            new Started(id).fire();

            state.getCurrentPlayers().stream()
                    .map(com.boardgamefiesta.api.domain.Player::getName)
                    .map(Player.Id::of)
                    .map(this::getPlayerById)
                    .flatMap(Optional::stream)
                    .forEach(this::beginTurn);
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
        }
    }

    private void beginTurn(Player player) {
        player.beginTurn(type == Type.TURN_BASED ? TURN_BASED_TURN_LIMIT : REALTIME_TURN_LIMIT);

        currentState.get()
                .map(CurrentState::getState)
                .flatMap(state -> state.getPlayerByName(player.getId().getId())
                        .flatMap(state::getTurn))
                .ifPresentOrElse(turns -> log.add(new LogEntry(player, LogEntry.Type.BEGIN_TURN_NR, List.of(turns))),
                        () -> log.add(new LogEntry(player, LogEntry.Type.BEGIN_TURN)));

        new BeginTurn(game.getId(), id, type, player.getUserId(), player.getTurnLimit().get(), started).fire();
    }

    private void endTurnInternal(Player player) {
        player.endTurn();

        if (player.getStatus() != Player.Status.LEFT) {
            currentState.get()
                    .map(CurrentState::getState)
                    .flatMap(state -> state.getPlayerByName(player.getId().getId())
                            .flatMap(state::getTurn))
                    .ifPresentOrElse(turns -> log.add(new LogEntry(player, LogEntry.Type.END_TURN_NR, List.of(turns))),
                            () -> log.add(new LogEntry(player, LogEntry.Type.END_TURN)));

            new EndTurn(id, player.getUserId(), Instant.now()).fire();
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

        runStateChange(state -> state.perform(state.getPlayerByName(player.getId().getId())
                .orElseThrow(NotPlayer::new), action, RANDOM));
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
        } catch (InGameException e) {
            throw new InGameError(game.getId(), e);
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
                .forEach(this::endTurnInternal);

        if (!state.isEnded()) {
            newCurrentPlayers.stream()
                    .filter(newCurrentPlayer -> !currentPlayers.contains(newCurrentPlayer))
                    .forEach(this::beginTurn);
        }

        afterStateChange();
    }

    public void skip(Player player) {
        checkStarted();

        checkTurn(player);

        log.add(new LogEntry(player, LogEntry.Type.SKIP));

        runStateChange(state -> state.skip(getPlayer(player), RANDOM));
    }

    public void endTurn(Player player) {
        checkStarted();

        runStateChange(state -> state.endTurn(getPlayer(player), RANDOM));
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

        log.add(new LogEntry(player, LogEntry.Type.LEFT));

        player.leave();

        if (status == Status.NEW) {
            players.remove(player);
        }

        new Left(id, userId, Instant.now()).fire();

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
            var player = getState().getPlayerByName(tablePlayer.getId().getId()).orElseThrow();

            runStateChange(state -> state.leave(player, RANDOM));

            if (status != Status.ENDED /* not automatically ended by state change */
                    && players.stream().filter(Player::isPlaying).count() < game.getMinNumberOfPlayers()) {
                // Game cannot continue with one less player

                // If the game is only against computer players, then just abandon
                // If the player has not played X number of turns yet, then just abandon
                if (hasMoreThanOneHumanPlayer() && getState().getTurn(player).orElse(0) > MAX_TURNS_TO_ABANDON) {
                    end();
                } else {
                    abandon();
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
            end();
        } else {
            assignScores();
        }
    }

    private void end() {
        status = Status.ENDED;
        ended = Instant.now();

        assignScores();

        log.add(new LogEntry(getPlayerByUserId(ownerId).orElseThrow(), LogEntry.Type.END));

        new Ended(id, ended).fire();
    }

    private void assignScores() {
        var state = getState();

        var winner = status == Status.ENDED ? state.ranking().get(0) : null;

        for (Player player : players) {
            if (player.isPlaying()) {
                state.getPlayerByName(player.getId().getId())
                        .map(state::score)
                        .ifPresent(score ->
                                player.assignScore(score, winner != null && winner.getName().equals(player.getId().getId())));
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

        player.accept();
        new Accepted(id, user.getId()).fire();

        log.add(new LogEntry(player, LogEntry.Type.ACCEPT));

        user.getColorPreferences().pickPreferredColor(this.getAvailableColors())
                .ifPresent(player::assignColor);

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

    public Set<PlayerColor> getAvailableColors() {
        var colors = new HashSet<>(game.getSupportedColors());

        players.forEach(player -> player.getColor().ifPresent(colors::remove));

        return Collections.unmodifiableSet(colors);
    }

    public void invite(User user) {
        checkNew();

        if (players.size() == maxNumberOfPlayers) {
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

    public void kick(@NonNull User.Id currentUserId, @NonNull Player player) {
        checkPlayer(player);

        var kickingPlayer = getPlayerByUserId(currentUserId)
                .filter(Player::isActive)
                .orElseThrow(NotPlayer::new);

        if (status == Status.NEW) {
            checkOwner(currentUserId);

            players.remove(player);
        } else {
            checkStarted();

            player.kick();
        }

        player.getUserId().ifPresent(userId -> {
            log.add(new LogEntry(kickingPlayer, LogEntry.Type.KICK, List.of(userId.getId())));

            new Kicked(this.id, userId, Instant.now()).fire();
        });

        afterPlayerLeft(player);
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

        runStateChange(state -> state.forceEndTurn(getPlayer(player), RANDOM));

        new ForcedEndTurn(id, userId, Instant.now()).fire();
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

    public void join(@NonNull User user) {
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

        var player = Player.accepted(user.getId());
        players.add(player);

        user.getColorPreferences().pickPreferredColor(this.getAvailableColors())
                .ifPresent(player::assignColor);

        log.add(new LogEntry(player, LogEntry.Type.JOIN, List.of(user.getId())));

        new Joined(id, user.getId()).fire();

        autoStartIfPossible();
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

        new VisibilityChanged(id).fire();
    }

    public void makePrivate() {
        checkNew();

        visibility = Visibility.PRIVATE;

        new VisibilityChanged(id).fire();
    }

    public void addComputer() {
        checkNew();

        if (players.size() == maxNumberOfPlayers) {
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

        new OptionsChanged(id).fire();
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

    public List<User.Id> getUserRanking() {
        return currentState.get()
                .map(CurrentState::getState)
                .map(State::ranking)
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

        return state.getPlayerByName(player.getId().getId()) // could be empty when player has left
                .map(state::stats);
    }

    public void changeMode(@NonNull Mode mode) {
        checkNew();

        this.mode = mode;

        new OptionsChanged(id).fire();
    }

    public void changeMinMaxNumberOfPlayers(int minNumberOfPlayers, int maxNumberOfPlayers) {
        checkNew();

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

        new OptionsChanged(id).fire();
    }

    public void changeAutoStart(boolean autoStart) {
        checkNew();

        this.autoStart = autoStart;

        new OptionsChanged(id).fire();
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
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
        @NonNull Instant timestamp;
    }

    @Value
    public static class ForcedEndTurn implements DomainEvent {
        @NonNull Table.Id tableId;
        @NonNull User.Id userId;
        @NonNull Instant timestamp;
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
        @NonNull Table.Id tableId;
        @NonNull Instant timestamp;
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
        @NonNull Instant timestamp;
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

    @Value
    public static class EndTurn implements DomainEvent {
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

}
