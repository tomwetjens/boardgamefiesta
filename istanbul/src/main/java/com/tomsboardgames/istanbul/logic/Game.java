
package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.EventListener;
import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import com.tomsboardgames.api.State;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Game implements Serializable, State {

    private static final long serialVersionUID = 1L;

    public static final Set<PlayerColor> SUPPORTED_COLORS = Set.of(PlayerColor.WHITE, PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE);

    @Getter
    @NonNull
    private final List<Player> players;

    @NonNull
    private final Map<Player, PlayerState> playerStates;

    @Getter
    @NonNull
    private final Layout layout;

    @Getter
    @NonNull
    private final Player startPlayer;

    @NonNull
    private final LinkedList<BonusCard> bonusCards;

    private final ActionQueue actionQueue;

    @Getter
    @NonNull
    private Player currentPlayer;

    @NonNull
    private Status status;

    private transient List<EventListener> eventListeners;

    public static Game start(@NonNull Set<Player> players, @NonNull LayoutType layoutType, @NonNull Random random) {
        var playerOrder = new ArrayList<>(players);
        Collections.shuffle(playerOrder, random);

        int playerCount = players.size();

        var layout = layoutType.createLayout(playerCount, random);

        layout.randomPlace(random).placeGovernor();
        layout.randomPlace(random).placeSmuggler();

        var startPlayer = playerOrder.get(0);

        var playerStates = IntStream.range(0, players.size())
                .boxed()
                .collect(Collectors.toMap(playerOrder::get, PlayerState::start));

        var bonusCards = new LinkedList<>(BonusCard.createDeck());
        Collections.shuffle(bonusCards, random);

        var actionQueue = new ActionQueue();
        actionQueue.addFirst(PossibleAction.mandatory(Action.Move.class));

        var game = new Game(
                playerOrder,
                playerStates,
                layout,
                startPlayer,
                bonusCards,
                actionQueue,
                startPlayer,
                Status.STARTED,
                new ArrayList<>());

        var fountain = layout.getFountain();
        var policeStation = layout.getPoliceStation();
        players.forEach(player -> {
            fountain.placeMerchant(Merchant.forPlayer(player), game);
            policeStation.placeFamilyMember(game, player);
        });

        if (playerCount == 2) {
            game.placeDummyMerchants();
        }

        return game;
    }

    private void placeDummyMerchants() {
        var smallMosque = layout.getSmallMosque();
        var greatMosque = layout.getGreatMosque();
        var gemstoneDealer = layout.getGemstoneDealer();

        var availableColors = SUPPORTED_COLORS.stream()
                .filter(color -> players.stream().noneMatch(player -> player.getColor() == color))
                .collect(Collectors.toCollection(LinkedList::new));
        Collections.shuffle(availableColors);

        var places = List.of(smallMosque, greatMosque, gemstoneDealer);
        places.forEach(place -> {
            var dummy = Merchant.dummy(availableColors.poll());

            place.placeMerchant(dummy, this);
        });
    }

    @Override
    public void serialize(OutputStream outputStream) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Game deserialize(InputStream inputStream) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (Game) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void perform(com.tomsboardgames.api.Action action, Random random) {
        perform((Action) action, random);
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        if (eventListeners == null) {
            // Could be null after deserialization
            eventListeners = new LinkedList<>();
        }
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(EventListener eventListener) {
        if (eventListeners != null) {
            // Could be null after deserialization
            eventListeners.remove(eventListener);
        }
    }

    @Override
    public int score(Player player) {
        return getPlayerState(player).getRubies();
    }

    @Override
    public Set<Player> winners() {
        var max = playerStates.entrySet().stream()
                .max(Comparator.<Map.Entry<Player, PlayerState>>comparingInt(entry -> entry.getValue().getRubies())
                        .thenComparingInt(entry -> entry.getValue().getLira())
                        .thenComparingInt(entry -> entry.getValue().getTotalGoods())
                        .thenComparingInt(entry -> entry.getValue().getBonusCards().size()))
                .map(Map.Entry::getValue)
                .orElseThrow();
        return playerStates.entrySet().stream()
                .filter(entry -> entry.getValue().getRubies() == max.getRubies()
                        && entry.getValue().getLira() == max.getLira()
                        && entry.getValue().getTotalGoods() == max.getTotalGoods()
                        && entry.getValue().getBonusCards().size() == max.getBonusCards().size())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public void perform(@NonNull Action action, @NonNull Random random) {
        ActionResult actionResult;

        if (canPerformAnyTime(action) || canPerformBeforeOrAfter(action)) {
            actionResult = action.perform(this, random);
        } else if (actionQueue.canPerform(action.getClass())) {
            actionResult = action.perform(this, random);
            actionQueue.perform(action.getClass());
        } else {
            throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
        }

        actionQueue.addFirst(actionResult.getFollowUpActions());

        if (!canPerformAnotherAction()) {
            endTurn(random);
        }
    }

    private boolean canPerformAnotherAction() {
        return !actionQueue.isEmpty() || !anyTimeActions().isEmpty() || !beforeOrAfterActions().isEmpty();
    }

    private boolean canPerformBeforeOrAfter(Action action) {
        return actionQueue.getCurrent().isEmpty() && beforeOrAfterActions().contains(action.getClass());
    }

    private boolean canPerformAnyTime(Action action) {
        return anyTimeActions().contains(action.getClass());
    }

    @Override
    public void skip(@NonNull Random random) {
        actionQueue.skip();

        if (actionQueue.isEmpty()) {
            endTurn(random);
        }
    }

    @Override
    public void endTurn(@NonNull Random random) {
        actionQueue.skipAll();

        nextPlayer();
    }

    private void nextPlayer() {
        if (status == Status.STARTED && currentPlayerState().hasMaxRubies(players.size())) {
            // Triggers end of game, finish the round
            status = Status.LAST_ROUND;
        }

        var currentPlayerIndex = players.indexOf(this.currentPlayer);
        var nextPlayer = players.get((currentPlayerIndex + 1) % players.size());
        var nextPlayerState = getPlayerState(nextPlayer);

        this.currentPlayer = nextPlayer;

        // Status transitions
        if (this.currentPlayer == startPlayer) {
            if (status == Status.LAST_ROUND) {
                // Finished last round
                status = Status.PLAY_LEFTOVER_BONUS_CARDS;
            } else if (status == Status.PLAY_LEFTOVER_BONUS_CARDS) {
                // All players have played last bonus cards (if applicable)
                status = Status.ENDED;
            }
        }

        if (status == Status.PLAY_LEFTOVER_BONUS_CARDS) {
            // If player does not have any bonus cards to play, go to the next player directly
            // This can go on to the next, etc. until the start player, if no players have bonus cards to play
            if (getPossibleActions().isEmpty()) {
                nextPlayer();
            }
        }

        if (!isEnded() && status != Status.PLAY_LEFTOVER_BONUS_CARDS) {
            this.actionQueue.addFirst(PossibleAction.mandatory(Action.Move.class));

            if (nextPlayerState.hasMosqueTile(MosqueTile.PAY_2_LIRA_TO_RETURN_ASSISTANT)) {
                // Once in a turn
                this.actionQueue.addAnyTime(PossibleAction.optional(Action.Pay2LiraToReturnAssistant.class));
            }
        }
    }

    @Override
    public boolean isEnded() {
        return status == Status.ENDED;
    }

    @Override
    public void leave(Player player) {
        if (players.contains(player)) {
            if (currentPlayer == player) {
                actionQueue.clear();
                nextPlayer();
            }

            players.remove(player);
        }
    }

    public Set<Class<? extends Action>> getPossibleActions() {
        var possibleActions = new HashSet<>(actionQueue.getPossibleActions());
        if (actionQueue.getCurrent().isEmpty()) {
            possibleActions.addAll(beforeOrAfterActions());
        }
        possibleActions.addAll(anyTimeActions());
        return Collections.unmodifiableSet(possibleActions);
    }

    /**
     * Actions that can be performed before or after an action during the current player's turn, but not during another action.
     */
    private Set<Class<? extends Action>> beforeOrAfterActions() {
        if (currentPlayerState().hasBonusCard(BonusCard.GAIN_1_GOOD)) {
            return Collections.singleton(Action.BonusCardGain1Good.class);
        }
        return Collections.emptySet();
    }

    /**
     * Actions that can be performed at any time during the current player's turn, also during other actions.
     */
    private Set<Class<? extends Action>> anyTimeActions() {
        if (currentPlayerState().hasBonusCard(BonusCard.TAKE_5_LIRA)) {
            return Collections.singleton(Action.BonusCardTake5Lira.class);
        }
        if (status != Status.PLAY_LEFTOVER_BONUS_CARDS) {
            if (currentPlayerState().hasBonusCard(BonusCard.FAMILY_MEMBER_TO_POLICE_STATION)) {
                return Collections.singleton(Action.PlaceFamilyMemberOnPoliceStation.class);
            }
            if (isFirstPhase() && currentPlayerState().hasBonusCard(BonusCard.RETURN_1_ASSISTANT)) {
                return Collections.singleton(Action.Return1Assistant.class);
            }
        }
        return Collections.emptySet();
    }

    private boolean isFirstPhase() {
        return actionQueue.canPerform(Action.LeaveAssistant.class) || actionQueue.canPerform(Action.Move.class);
    }

    PlayerState currentPlayerState() {
        return playerStates.get(currentPlayer);
    }


    Place getCurrentPlace() {
        return getCurrentPlace(currentPlayer.getColor());
    }

    Place getCurrentPlace(PlayerColor playerColor) {
        return layout.currentPlaceOfMerchant(playerColor);
    }

    Place getFamilyMemberCurrentPlace(Player player) {
        return layout.currentPlaceOfFamilyMember(player);
    }

    BonusCard drawBonusCard(Random random) {
        if (bonusCards.isEmpty()) {
            bonusCards.addAll(layout.getCaravansary().takeDiscardPile());
            Collections.shuffle(bonusCards, random);
        }
        return bonusCards.poll();
    }

    ActionResult moveMerchant(@NonNull Merchant merchant, @NonNull Place to, int atLeast, int atMost) {
        var from = getCurrentPlace(merchant.getColor());

        var dist = layout.distance(from, to);
        if (dist < atLeast && dist > atMost) {
            throw new IstanbulException(IstanbulError.PLACE_NOT_REACHABLE);
        }

        from.removeMerchant(merchant);

        return to.placeMerchant(merchant, this);
    }

    void fireEvent(IstanbulEvent event) {
        if (eventListeners != null) {
            // Could be null after deserialization
            eventListeners.forEach(eventListener -> eventListener.event(event));
        }
    }

    Place randomPlace(@NonNull Random random) {
        return layout.randomPlace(random);
    }

    public Place place(int x, int y) {
        return layout.place(x, y);
    }

    Place place(Predicate<Place> predicate) {
        return layout.place(predicate);
    }

    public PlayerState getPlayerState(Player player) {
        return playerStates.get(player);
    }

    public Place.GreatMosque getGreatMosque() {
        return layout.getGreatMosque();
    }

    public Place.SmallMosque getSmallMosque() {
        return layout.getSmallMosque();
    }

    public Place.LargeMarket getLargeMarket() {
        return layout.getLargeMarket();
    }

    public Place.SmallMarket getSmallMarket() {
        return layout.getSmallMarket();
    }

    public Place.SultansPalace getSultansPalace() {
        return layout.getSultansPalace();
    }

    public Place.GemstoneDealer getGemstoneDealer() {
        return layout.getGemstoneDealer();
    }

    public Place.TeaHouse getTeaHouse() {
        return layout.getTeaHouse();
    }

    public Place.BlackMarket getBlackMarket() {
        return layout.getBlackMarket();
    }

    public Place.Fountain getFountain() {
        return layout.getFountain();
    }

    public Place.Caravansary getCaravansary() {
        return layout.getCaravansary();
    }

    public Place.PostOffice getPostOffice() {
        return layout.getPostOffice();
    }

    public Place.Wainwright getWainwright() {
        return layout.getWainwright();
    }

    public Place.SpiceWarehouse getSpiceWarehouse() {
        return layout.getSpiceWarehouse();
    }

    public Place.FruitWarehouse getFruitWarehouse() {
        return layout.getFruitWarehouse();
    }

    public Place.FabricWarehouse getFabricWarehouse() {
        return layout.getFabricWarehouse();
    }

    public Place.PoliceStation getPoliceStation() {
        return layout.getPoliceStation();
    }

    Merchant getCurrentMerchant() {
        return getCurrentPlace().getMerchant(currentPlayer.getColor());
    }

    enum Status {
        STARTED,
        LAST_ROUND,
        PLAY_LEFTOVER_BONUS_CARDS,
        ENDED
    }
}
