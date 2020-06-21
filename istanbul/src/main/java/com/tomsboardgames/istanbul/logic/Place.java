package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import lombok.*;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Place implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private final int number;

    @Getter
    private final Set<Merchant> merchants = new HashSet<>();

    @Getter
    private final Map<PlayerColor, Integer> assistants = new HashMap<>();

    @Getter
    private final Set<Player> familyMembers = new HashSet<>();

    @Getter
    private boolean governor;

    @Getter
    private boolean smuggler;

    protected abstract Optional<PossibleAction> getPossibleAction(Game game);

    ActionResult placeMerchant(@NonNull Merchant merchant, @NonNull Game game) {
        if (!merchants.add(merchant)) {
            throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
        }

        // One of these options:
        // 1. If assistant already present, players "picks up assistant". Return place actions
        // 2. If no assistant already present, return "Leave assistant" action
        //   2a. If performed, the "Leave assistant" actions adds place actions to the stack
        //   2b. If skipped, it ends turn (because stack is empty)

        var numberOfAssistants = assistants.getOrDefault(merchant.getColor(), 0);

        if (numberOfAssistants > 0) {
            // Picks up assistants

            merchant.returnAssistants(numberOfAssistants);
            assistants.put(merchant.getColor(), 0);

            if (mustPayOtherMerchants()) {
                return ActionResult.followUp(PossibleAction.optional(Action.PayOtherMerchants.class));
            }

            return placeActions(game);
        } else if (merchant.getAssistants() > 0) {
            return ActionResult.followUp(PossibleAction.optional(Action.LeaveAssistant.class));
        } else {
            // No assistants left to leave
            return ActionResult.none();
        }
    }

    void removeMerchant(Merchant merchant) {
        if (!merchants.remove(merchant)) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }
    }

    ActionResult leaveAssistant(Merchant merchant, Game game) {
        if (!merchants.contains(merchant)) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }

        merchant.removeAssistant();

        var currentAssistants = assistants.getOrDefault(merchant.getColor(), 0);
        assistants.put(merchant.getColor(), currentAssistants + 1);

        if (mustPayOtherMerchants()) {
            return ActionResult.followUp(PossibleAction.optional(Action.PayOtherMerchants.class));
        }

        return placeActions(game);
    }

    ActionResult placeFamilyMember(Game game, Player player) {
        if (!familyMembers.add(player)) {
            throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
        }
        return placeActions(game);
    }

    ActionResult placeActions(Game game) {
        // Place actions:
        // 1. If there are other merchants present, then return "Pay Other Merchants" action
        //   1a. If player pays other merchants, then return actions 2+3
        //   1b. If player skips, then ends turn (because stack is empty)
        // 2. Action of the place
        // 3. Any governor, smuggler or family action
        return getPossibleAction(game)
                .map(ActionResult::new)
                .orElse(ActionResult.none())
                .andThen(encounterActions(game));
    }

    protected boolean mustPayOtherMerchants() {
        return merchants.size() > 1;
    }

    private ActionResult encounterActions(Game game) {
        var actions = new HashSet<PossibleAction>();

        if (governor) {
            actions.add(PossibleAction.optional(Action.Governor.class));
        }
        if (smuggler) {
            actions.add(PossibleAction.optional(Action.Smuggler.class));
        }

        var numberOfFamilyMembersToCatch = (int) familyMembersToCatch(game.getCurrentPlayer()).count();
        if (numberOfFamilyMembersToCatch > 0) {
            actions.add(PossibleAction.repeat(numberOfFamilyMembersToCatch, numberOfFamilyMembersToCatch,
                    PossibleAction.choice(Set.of(Action.CatchFamilyMemberForBonusCard.class, Action.CatchFamilyMemberFor3Lira.class))));
        }

        return !actions.isEmpty() ? ActionResult.followUp(PossibleAction.any(actions)) : ActionResult.none();
    }

    void placeGovernor() {
        if (this.governor) {
            throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
        }
        this.governor = true;
    }

    void placeSmuggler() {
        if (this.smuggler) {
            throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
        }
        this.smuggler = true;
    }

    void takeGovernor() {
        if (!governor) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }
        governor = false;
    }

    void takeSmuggler() {
        if (!smuggler) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }
        smuggler = false;
    }

    void takeFamilyMember(Player player) {
        if (!familyMembers.remove(player)) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }
    }

    Stream<Player> familyMembersToCatch(Player currentPlayer) {
        return familyMembers.stream()
                .filter(familyMember -> familyMember != currentPlayer);
    }

    void catchFamilyMember(Game game) {
        var policeStation = game.getPoliceStation();

        var otherFamilyMember = familyMembersToCatch(game.getCurrentPlayer())
                .findAny()
                .orElseThrow(() -> new IstanbulException(IstanbulError.NO_FAMILY_MEMBER_TO_CATCH));

        takeFamilyMember(otherFamilyMember);
        policeStation.placeFamilyMember(game, otherFamilyMember);
    }

    Merchant getMerchant(PlayerColor color) {
        return merchants.stream()
                .filter(merchant -> merchant.getColor() == color)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Merchant not at place"));
    }

    public static class Wainwright extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        Wainwright() {
            super(1);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.BuyWheelbarrowExtension.class));
        }
    }

    public static class FabricWarehouse extends Place implements Serializable {
        private static final long serialVersionUID = 1L;

        FabricWarehouse() {
            super(2);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.MaxFabric.class));
        }
    }

    public static class SpiceWarehouse extends Place implements Serializable {
        private static final long serialVersionUID = 1L;

        SpiceWarehouse() {
            super(3);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.MaxSpice.class));
        }
    }

    public static class FruitWarehouse extends Place implements Serializable {
        private static final long serialVersionUID = 1L;

        FruitWarehouse() {
            super(4);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.MaxFruit.class));
        }
    }

    public static class PostOffice extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        private final boolean[] indicators = new boolean[4];

        PostOffice() {
            super(5);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.UsePostOffice.class));
        }

        ActionResult use(Game game) {
            // row 1: fabric, 2 lira, blue, 2 lira
            // row 2: spice, 1 lira, fruit, 1 lira

            var currentPlayerState = game.currentPlayerState();
            currentPlayerState.addGoods(indicators[0] ? GoodsType.SPICE : GoodsType.FABRIC, 1);
            currentPlayerState.gainLira(indicators[1] ? 1 : 2);
            currentPlayerState.addGoods(indicators[2] ? GoodsType.FRUIT : GoodsType.BLUE, 1);
            currentPlayerState.gainLira(indicators[3] ? 1 : 2);

            var leftMost = leftMostInTopRow();
            if (leftMost >= 0) {
                // Move from top row to bottom row
                indicators[leftMost] = true;
            } else {
                // Move all from bottom row back to top row
                Arrays.fill(indicators, false);
            }

            return ActionResult.none();
        }

        public List<Boolean> getIndicators() {
            return List.of(indicators[0], indicators[1], indicators[2], indicators[3]);
        }

        private int leftMostInTopRow() {
            for (int i = 0; i < indicators.length; i++) {
                if (!indicators[i]) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static class Caravansary extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        @Getter
        private final List<BonusCard> discardPile = new LinkedList<>();

        Caravansary() {
            super(6);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.whenThen(PossibleAction.optional(Action.Take2BonusCards.class),
                    PossibleAction.mandatory(Action.DiscardBonusCard.class), 0, 1));
        }

        List<BonusCard> takeDiscardPile() {
            var result = List.copyOf(this.discardPile);
            this.discardPile.clear();
            return result;
        }

        public BonusCard drawBonusCard() {
            if (discardPile.isEmpty()) {
                throw new IstanbulException(IstanbulError.NO_BONUS_CARD_AVAILABLE);
            }
            return discardPile.remove(0);
        }

    }

    public static class Fountain extends Place implements Serializable {
        private static final long serialVersionUID = 1L;

        Fountain() {
            super(7);
        }

        @Override
        ActionResult placeMerchant(Merchant merchant, Game game) {
            super.placeMerchant(merchant, game);

            // Immediately activate and return place actions
            return placeActions(game);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.ReturnAllAssistants.class));
        }

        @Override
        protected boolean mustPayOtherMerchants() {
            return false;
        }
    }

    void returnAssistants(Merchant merchant) {
        var numberOfAssistants = assistants.put(merchant.getColor(), 0);
        if (numberOfAssistants != null) {
            merchant.returnAssistants(numberOfAssistants);
        }
    }

    void returnAssistant(Merchant merchant) {
        var numberOfAssistants = assistants.getOrDefault(merchant.getColor(), 0);

        if (numberOfAssistants == 0) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }
        assistants.put(merchant.getColor(), numberOfAssistants - 1);

        merchant.returnAssistants(1);
    }

    public static class BlackMarket extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        BlackMarket() {
            super(8);
        }

        static void rollForBlueGoods(@NonNull PlayerState playerState, @NonNull Random random) {
            var dice = random.nextInt(12);

            playerState.addGoods(GoodsType.BLUE,
                    dice > 10 ? 3 : dice > 8 ? 2 : dice > 6 ? 1 : 0);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.any(Set.of(
                    PossibleAction.choice(Set.of(Action.Take1Fabric.class, Action.Take1Spice.class, Action.Take1Fruit.class)),
                    PossibleAction.optional(Action.RollForBlueGoods.class))));
        }
    }

    public static class TeaHouse extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        TeaHouse() {
            super(9);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.GuessAndRollForLira.class));
        }

        void guessAndRoll(@NonNull Game game, int guess, @NonNull Random random) {
            var currentPlayer = game.getCurrentPlayer();

            game.fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.GUESSED, guess));

            var die1 = random.nextInt(6);
            var die2 = random.nextInt(6);

            game.fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.ROLLED, die1, die2));

            var currentPlayerState = game.getPlayerState(currentPlayer);
            var lira = 2;

            if (die1 + die2 >= guess) {
                lira = guess;
            } else {
                if (currentPlayerState.hasMosqueTile(MosqueTile.TURN_OR_REROLL_DICE)) {
                    // Can we turn one die to make the guess?
                    if (die1 + 4 >= guess || die2 + 4 >= guess) {
                        game.fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.TURNED_DIE));

                        lira = guess;
                    }

                    // Else reroll automatically
                    die1 = random.nextInt(6);
                    die2 = random.nextInt(6);

                    game.fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.ROLLED, die1, die2));

                    if (die1 + die2 >= guess) {
                        lira = guess;
                    }
                }
            }

            currentPlayerState.gainLira(lira);
        }
    }

    public static class Market extends Place {

        private static final long serialVersionUID = 1L;

        private final LinkedList<Map<GoodsType, Integer>> demands;
        private final int[] rewards;

        protected Market(int number, Collection<Map<GoodsType, Integer>> demands, int[] rewards,
                         Random random) {
            super(number);

            this.demands = new LinkedList<>(demands);
            Collections.shuffle(this.demands, random);

            this.rewards = rewards;
        }

        public Map<GoodsType, Integer> getDemand() {
            return demands.getFirst();
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.SellGoods.class));
        }

        void sellDemandGoods(Game game, Map<GoodsType, Integer> goods) {
            var currentPlayerState = game.currentPlayerState();
            var demand = demands.getFirst();

            var numberOfGoods = goods.entrySet().stream()
                    .mapToInt(entry -> currentPlayerState.removeGoods(entry.getKey(),
                            Math.min(entry.getValue(), demand.get(entry.getKey()))))
                    .sum();

            sell(currentPlayerState, numberOfGoods);
        }

        protected void sell(PlayerState currentPlayerState, int numberOfGoods) {
            if (numberOfGoods > 0) {
                currentPlayerState.gainLira(rewards[numberOfGoods - 1]);

                demands.addLast(demands.remove(0));
            }
        }

    }

    public static class SmallMarket extends Market implements Serializable {

        private static final long serialVersionUID = 1L;

        private SmallMarket(Random random) {
            super(11, List.of(
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 2, GoodsType.FRUIT, 1, GoodsType.BLUE, 1),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 2, GoodsType.FRUIT, 2, GoodsType.BLUE, 0),
                    Map.of(GoodsType.FABRIC, 0, GoodsType.SPICE, 2, GoodsType.FRUIT, 2, GoodsType.BLUE, 1),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 1, GoodsType.FRUIT, 2, GoodsType.BLUE, 1),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 3, GoodsType.FRUIT, 1, GoodsType.BLUE, 0)),
                    new int[]{2, 5, 9, 14, 20}, random);
        }

        void sellAnyGoods(Game game, Map<GoodsType, Integer> goods) {
            var currentPlayerState = game.currentPlayerState();

            var numberOfGoods = goods.entrySet().stream()
                    .mapToInt(entry -> currentPlayerState.removeGoods(entry.getKey(), entry.getValue()))
                    .sum();

            sell(currentPlayerState, numberOfGoods);
        }

        static SmallMarket randomize(@NonNull Random random) {
            return new SmallMarket(random);
        }
    }

    public static class LargeMarket extends Market implements Serializable {

        private static final long serialVersionUID = 1L;

        private LargeMarket(Random random) {
            super(10, List.of(
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 1, GoodsType.FRUIT, 1, GoodsType.BLUE, 2),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 1, GoodsType.FRUIT, 0, GoodsType.BLUE, 3),
                    Map.of(GoodsType.FABRIC, 2, GoodsType.SPICE, 1, GoodsType.FRUIT, 0, GoodsType.BLUE, 2),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 0, GoodsType.FRUIT, 1, GoodsType.BLUE, 3),
                    Map.of(GoodsType.FABRIC, 2, GoodsType.SPICE, 0, GoodsType.FRUIT, 1, GoodsType.BLUE, 2)),
                    new int[]{3, 7, 12, 18, 25}, random);
        }

        static LargeMarket randomize(@NonNull Random random) {
            return new LargeMarket(random);
        }
    }

    public static class PoliceStation extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        PoliceStation() {
            super(12);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.SendFamilyMember.class));
        }

        @Override
        Stream<Player> familyMembersToCatch(Player currentPlayer) {
            // Family members already at the police station cannot be caught
            return Stream.empty();
        }
    }

    public static class Mosque extends Place {

        private final MosqueTileStack a;
        private final MosqueTileStack b;

        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        private static class MosqueTileStack implements Serializable {

            private static final long serialVersionUID = 1L;

            @Getter
            private final MosqueTile mosqueTile;
            private final List<Integer> goodsCounts;

            static MosqueTileStack forPlayerCount(@NonNull MosqueTile mosqueTile, int playerCount) {
                return new MosqueTileStack(mosqueTile, new LinkedList<>(initialGoodsCounts(playerCount)));
            }

            boolean isAvailable() {
                return !goodsCounts.isEmpty();
            }

            Optional<Integer> getGoodsCount() {
                return goodsCounts.stream().findFirst();
            }

            void take() {
                goodsCounts.remove(0);
            }

            private static List<Integer> initialGoodsCounts(int playerCount) {
                switch (playerCount) {
                    case 2:
                        return Arrays.asList(2, 4);
                    case 3:
                        return Arrays.asList(2, 3, 4);
                    default:
                        return Arrays.asList(2, 3, 4, 5);
                }
            }

        }

        Mosque(int number, int playerCount, @NonNull MosqueTile a, @NonNull MosqueTile b) {
            super(number);

            this.a = MosqueTileStack.forPlayerCount(a, playerCount);
            this.b = MosqueTileStack.forPlayerCount(b, playerCount);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            if (a.isAvailable() || b.isAvailable()) {
                return Optional.of(PossibleAction.optional(Action.TakeMosqueTile.class));
            }
            return Optional.empty();
        }

        public Optional<Integer> getA() {
            return a.getGoodsCount();
        }

        public Optional<Integer> getB() {
            return b.getGoodsCount();
        }

        ActionResult takeMosqueTile(@NonNull MosqueTile mosqueTile, @NonNull Game game) {
            MosqueTileStack stack = getStack(mosqueTile);

            var goodsCount = stack.getGoodsCount()
                    .orElseThrow(() -> new IstanbulException(IstanbulError.MOSQUE_TILE_NOT_AVAILABLE));

            var currentPlayerState = game.currentPlayerState();
            if (!currentPlayerState.hasAtLeastGoods(mosqueTile.getGoodsType(), goodsCount)) {
                throw new IstanbulException(IstanbulError.NOT_ENOUGH_GOODS);
            }

            currentPlayerState.removeGoods(mosqueTile.getGoodsType(), 1);
            currentPlayerState.addMosqueTile(mosqueTile);

            stack.take();

            if (hasBothMosqueTiles(currentPlayerState)) {
                currentPlayerState.gainRubies(1);
            }

            return mosqueTile.afterAcquire(game);
        }

        private boolean hasBothMosqueTiles(PlayerState playerState) {
            return playerState.hasMosqueTile(a.getMosqueTile())
                    && playerState.hasMosqueTile(b.getMosqueTile());
        }

        private MosqueTileStack getStack(MosqueTile mosqueTile) {
            if (a.getMosqueTile() == mosqueTile) {
                return this.a;
            } else if (b.getMosqueTile() == mosqueTile) {
                return this.b;
            } else {
                throw new IstanbulException(IstanbulError.MOSQUE_TILE_NOT_AVAILABLE);
            }
        }

    }

    public static class SmallMosque extends Mosque implements Serializable {

        private static final long serialVersionUID = 1L;

        private SmallMosque(int playerCount) {
            super(14, playerCount,
                    MosqueTile.TURN_OR_REROLL_DICE,
                    MosqueTile.PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD);
        }

        static SmallMosque forPlayerCount(int playerCount) {
            return new SmallMosque(playerCount);
        }
    }

    public static class GreatMosque extends Mosque implements Serializable {

        private static final long serialVersionUID = 1L;

        private GreatMosque(int playerCount) {
            super(15, playerCount,
                    MosqueTile.PAY_2_LIRA_TO_RETURN_ASSISTANT,
                    MosqueTile.EXTRA_ASSISTANT);
        }

        static GreatMosque forPlayerCount(int playerCount) {
            return new GreatMosque(playerCount);
        }
    }


    public static class SultansPalace extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        private static final List<Optional<GoodsType>> REQUIRED_GOODS = List.of(
                Optional.of(GoodsType.BLUE),
                Optional.of(GoodsType.FABRIC),
                Optional.of(GoodsType.SPICE),
                Optional.of(GoodsType.FRUIT),
                Optional.empty(),
                Optional.of(GoodsType.BLUE),
                Optional.of(GoodsType.FABRIC),
                Optional.of(GoodsType.SPICE),
                Optional.of(GoodsType.FRUIT),
                Optional.empty()
        );

        @Getter
        private int uncovered;

        private SultansPalace(int uncovered) {
            super(13);

            this.uncovered = uncovered;
        }

        static SultansPalace forPlayerCount(int playerCount) {
            return withUncovered(playerCount > 3 ? 4 : 5);
        }

        static SultansPalace withUncovered(int uncovered) {
            if (uncovered < 4 || uncovered > 11) {
                throw new IllegalArgumentException("Number of uncovered is out of range");
            }
            return new SultansPalace(uncovered);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.DeliverToSultan.class));
        }

        ActionResult deliverToSultan(PlayerState playerState) {
            if (uncovered > 10) {
                throw new IstanbulException(IstanbulError.NO_RUBY_AVAILABLE);
            }

            var requiredGoodsByType = REQUIRED_GOODS.stream()
                    .limit(uncovered)
                    .flatMap(Optional::stream)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            var numberOfAnyGoodsType = (int) REQUIRED_GOODS.stream()
                    .limit(uncovered)
                    .filter(Optional::isEmpty)
                    .count();

            // Check if player has all the required goods types,
            // Also check the total number of goods, in case "any" goods types are required
            if (!hasEnoughGoods(playerState, requiredGoodsByType) || playerState.getTotalGoods() < uncovered) {
                throw new IstanbulException(IstanbulError.NOT_ENOUGH_GOODS);
            }

            // First pay all the required goods types
            requiredGoodsByType.forEach((goodsType, amount) ->
                    playerState.removeGoods(goodsType, amount.intValue()));

            // Because we checked the player has enough goods, give the ruby already
            // even though the player still needs to choose which of the "any" goods to pay
            playerState.gainRubies(1);

            uncovered++;

            // Make sure the player pays the remaining "any" goods
            return ActionResult.followUp(PossibleAction.repeat(numberOfAnyGoodsType, numberOfAnyGoodsType,
                    PossibleAction.choice(Set.of(
                            Action.Pay1Fabric.class,
                            Action.Pay1Fruit.class,
                            Action.Pay1Spice.class,
                            Action.Pay1Blue.class))));
        }

        private boolean hasEnoughGoods(PlayerState playerState, Map<GoodsType, Long> requiredGoodsByType) {
            return requiredGoodsByType.entrySet().stream()
                    .allMatch(goods -> playerState.hasAtLeastGoods(goods.getKey(), goods.getValue().intValue()));
        }
    }

    public static class GemstoneDealer extends Place implements Serializable {

        private static final long serialVersionUID = 1L;

        @Getter
        private int cost;

        private GemstoneDealer(int cost) {
            super(16);
            this.cost = cost;
        }

        static GemstoneDealer forPlayerCount(int playerCount) {
            return withCost(playerCount == 2 ? 16 : playerCount == 3 ? 15 : 13);
        }

        static GemstoneDealer withCost(int cost) {
            if (cost < 12 || cost > 24) {
                throw new IllegalArgumentException("Cost out of range");
            }
            return new GemstoneDealer(cost);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Game game) {
            return Optional.of(PossibleAction.optional(Action.BuyRuby.class));
        }

        void buy(PlayerState playerState) {
            if (cost > 23) {
                throw new IstanbulException(IstanbulError.NO_RUBY_AVAILABLE);
            }

            playerState.payLira(cost);
            playerState.gainRubies(1);

            cost++;
        }
    }

}
