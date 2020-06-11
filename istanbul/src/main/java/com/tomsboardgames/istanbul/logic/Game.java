package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.EventListener;
import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import com.tomsboardgames.api.State;
import lombok.Getter;
import lombok.NonNull;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Game implements Serializable, State {

    private static final long serialVersionUID = 1L;

    public static final Set<PlayerColor> SUPPORTED_COLORS = Set.of(PlayerColor.WHITE, PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE);

    private final ActionQueue actionQueue = new ActionQueue();

    @Getter
    private final List<Player> players;

    @Getter
    private final Place[][] layout;

    private final Map<Player, PlayerState> playerStates = new HashMap<>();
    private final Map<PlayerColor, Place> currentPlaces = new HashMap<>();

    private final LinkedList<BonusCard> bonusCards;

    @Getter
    private final Player startPlayer;

    @Getter
    private Player currentPlayer;

    private boolean lastRound;

    private transient List<EventListener> eventListeners = new ArrayList<>();

    public Game(@NonNull Set<Player> players, @NonNull LayoutType layoutType, @NonNull Random random) {
        this.players = new ArrayList<>(players);
        Collections.shuffle(this.players, random);

        int playerCount = players.size();

        this.layout = layoutType.createLayout(playerCount, random);

        this.startPlayer = this.players.get(0);
        this.currentPlayer = this.startPlayer;

        var fountain = getPlace(Place.Fountain.class);

        for (int i = 0; i < this.players.size(); i++) {
            var player = this.players.get(i);

            var merchant = Merchant.forPlayer(player);

            playerStates.put(player, new PlayerState(2 + i, merchant));

            fountain.placeMerchant(merchant, this);
            currentPlaces.put(player.getColor(), fountain);
        }

        var policeStation = getPlace(Place.PoliceStation.class);
        this.players.forEach(player -> policeStation.sendFamilyMember(this, player));

        if (playerCount == 2) {
            placeDummyMerchants();
        }

        randomPlace(random).placeGovernor();
        randomPlace(random).placeSmuggler();

        this.bonusCards = new LinkedList<>(BonusCard.createDeck());
        Collections.shuffle(this.bonusCards, random);

        this.actionQueue.addFirst(PossibleAction.mandatory(Action.Move.class));
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

    Place randomPlace(@NonNull Random random) {
        var x = random.nextInt(layout.length);
        var y = random.nextInt(layout[x].length);
        return layout[x][y];
    }

    private void placeDummyMerchants() {
        var smallMosque = getPlace(Place.SmallMosque.class);
        var greatMosque = getPlace(Place.GreatMosque.class);
        var gemstoneDealer = getPlace(Place.GemstoneDealer.class);

        var availableColors = SUPPORTED_COLORS.stream()
                .filter(color -> this.players.stream().noneMatch(player -> player.getColor() == color))
                .collect(Collectors.toCollection(LinkedList::new));
        Collections.shuffle(availableColors);

        var places = List.of(smallMosque, greatMosque, gemstoneDealer);
        places.forEach(place -> {
            var dummy = Merchant.dummy(availableColors.poll());

            place.placeMerchant(dummy, this);
            currentPlaces.put(dummy.getColor(), place);
        });
    }

    @Override
    public void perform(com.tomsboardgames.api.Action action, Random random) {
        perform((Action) action, random);
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        if (eventListeners == null) {
            eventListeners = new LinkedList<>();
        }
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(EventListener eventListener) {
        if (eventListeners != null) {
            eventListeners.remove(eventListener);
        }
    }

    @Override
    public int score(Player player) {
        // TODO
        return 0;
    }

    @Override
    public Set<Player> winners() {
        return null;
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
        var currentPlayerState = currentPlayerState();
        if (currentPlayerState.getRubies() == 6 ||
                (currentPlayerState.getRubies() == 5 && players.size() > 2)) {
            // Triggers end of game, finish the round
            lastRound = true;
        }

        var currentPlayerIndex = players.indexOf(this.currentPlayer);
        this.currentPlayer = players.get((currentPlayerIndex + 1) % players.size());

        if (!isEnded()) {
            this.actionQueue.addFirst(PossibleAction.mandatory(Action.Move.class));

            var nextPlayerState = currentPlayerState();

            if (nextPlayerState.hasMosqueTile(MosqueTile.PAY_2_LIRA_TO_RETURN_ASSISTANT)) {
                // Once in a turn
                this.actionQueue.addAnyTime(PossibleAction.optional(Action.Pay2LiraToReturnAssistant.class));
            }
        }
    }

    @Override
    public boolean isEnded() {
        return lastRound && currentPlayer == startPlayer;
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
        return Collections.emptySet();
    }

    PlayerState currentPlayerState() {
        return playerStates.get(currentPlayer);
    }

    Place getPlace(int x, int y) {
        return layout[x][y];
    }

    private int distance(Place from, Place to) {
        for (int x1 = 0; x1 < layout.length; x1++) {
            for (int y1 = 0; y1 < layout[x1].length; y1++) {
                if (layout[x1][y1] == from) {
                    for (int x2 = 0; x2 < layout.length; x2++) {
                        for (int y2 = 0; y2 < layout[x2].length; y2++) {
                            if (layout[x2][y2] == to) {
                                return Math.abs(x1 - x2) + Math.abs(y1 - y2);
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("Place not found");
    }

    Place getCurrentPlace() {
        return getCurrentPlace(currentPlayer.getColor());
    }

    Place getCurrentPlace(PlayerColor playerColor) {
        return currentPlaces.get(playerColor);
    }

    BonusCard drawBonusCard(Random random) {
        if (bonusCards.isEmpty()) {
            bonusCards.addAll(getPlace(Place.Caravansary.class).takeDiscardPile());
            Collections.shuffle(bonusCards, random);
        }
        return bonusCards.poll();
    }

    Place getPlace(Predicate<Place> predicate) {
        for (Place[] places : layout) {
            for (Place place : places) {
                if (predicate.test(place)) {
                    return place;
                }
            }
        }
        throw new IllegalArgumentException("Place not found");
    }

    ActionResult moveMerchant(@NonNull Merchant merchant, @NonNull Place to, int atLeast, int atMost) {
        var from = getCurrentPlace(merchant.getColor());

        var dist = distance(from, to);
        if (dist < atLeast && dist > atMost) {
            throw new IstanbulException(IstanbulError.PLACE_NOT_REACHABLE);
        }

        from.takeMerchant(merchant);

        currentPlaces.put(merchant.getColor(), to);

        return to.placeMerchant(merchant, this);
    }

    @SuppressWarnings("unchecked")
    private <T extends Place> T getPlace(Class<T> clazz) {
        for (Place[] places : layout) {
            for (Place place : places) {
                if (clazz == place.getClass()) {
                    return (T) place;
                }
            }
        }
        throw new IllegalArgumentException("Place not found: " + clazz);
    }

    public PlayerState getPlayerState(Player player) {
        return playerStates.get(player);
    }

    public Place.GreatMosque getGreatMosque() {
        return getPlace(Place.GreatMosque.class);
    }

    public Place.SmallMosque getSmallMosque() {
        return getPlace(Place.SmallMosque.class);
    }

    public Place.LargeMarket getLargeMarket() {
        return getPlace(Place.LargeMarket.class);
    }

    public Place.SmallMarket getSmallMarket() {
        return getPlace(Place.SmallMarket.class);
    }

    public Place.SultansPalace getSultansPalace() {
        return getPlace(Place.SultansPalace.class);
    }

    public Place.GemstoneDealer getGemstoneDealer() {
        return getPlace(Place.GemstoneDealer.class);
    }

    public Place.TeaHouse getTeaHouse() {
        return getPlace(Place.TeaHouse.class);
    }

    public Place.BlackMarket getBlackMarket() {
        return getPlace(Place.BlackMarket.class);
    }

    public Place.Fountain getFountain() {
        return getPlace(Place.Fountain.class);
    }

    public Place.Caravansary getCaravansary() {
        return getPlace(Place.Caravansary.class);
    }

    public Place.PostOffice getPostOffice() {
        return getPlace(Place.PostOffice.class);
    }

    public Place.Wainwright getWainwright() {
        return getPlace(Place.Wainwright.class);
    }

    public Place.SpiceWarehouse getSpiceWarehouse() {
        return getPlace(Place.SpiceWarehouse.class);
    }

    public Place.FruitWarehouse getFruitWarehouse() {
        return getPlace(Place.FruitWarehouse.class);
    }

    public Place.FabricWarehouse getFabricWarehouse() {
        return getPlace(Place.FabricWarehouse.class);
    }

    public Place.PoliceStation getPoliceStation() {
        return getPlace(Place.PoliceStation.class);
    }

    void fireEvent(Player player, IstanbulEvent event, int... values) {
        // TODO
    }
}
