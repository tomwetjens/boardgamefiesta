package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Place {

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

    protected abstract Optional<PossibleAction> getPossibleAction(Istanbul game);

    ActionResult placeMerchant(@NonNull Merchant merchant, @NonNull Istanbul game) {
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

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PICK_UP_ASSISTANT));

            merchant.returnAssistants(numberOfAssistants);
            assistants.put(merchant.getColor(), 0);

            if (mustPayOtherMerchants()) {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MUST_PAY_OTHER_MERCHANTS));
                return ActionResult.followUp(PossibleAction.optional(Action.PayOtherMerchants.class), true);
            }

            return placeActions(game);
        } else if (merchant.getAssistants() > 0) {
            return ActionResult.followUp(PossibleAction.optional(Action.LeaveAssistant.class), true);
        } else {
            // No assistants left to leave
            return ActionResult.none(true);
        }
    }

    void removeMerchant(Merchant merchant) {
        if (!merchants.remove(merchant)) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }
    }

    ActionResult leaveAssistant(Merchant merchant, Istanbul game) {
        if (!merchants.contains(merchant)) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }

        merchant.removeAssistant();

        var currentAssistants = assistants.getOrDefault(merchant.getColor(), 0);
        assistants.put(merchant.getColor(), currentAssistants + 1);

        if (mustPayOtherMerchants()) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MUST_PAY_OTHER_MERCHANTS));
            return ActionResult.followUp(PossibleAction.optional(Action.PayOtherMerchants.class), true);
        }

        return placeActions(game);
    }

    ActionResult placeFamilyMember(Istanbul game, Player player) {
        if (!familyMembers.add(player)) {
            throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
        }
        return placeActions(game);
    }

    ActionResult placeActions(Istanbul game) {
        // Place actions:
        // 1. If there are other merchants present, then return "Pay Other Merchants" action
        //   1a. If player pays other merchants, then return actions 2+3
        //   1b. If player skips, then ends turn (because stack is empty)
        // 2. Action of the place
        // 3. Any governor, smuggler or family action
        return getPossibleAction(game)
                .map(possibleAction -> ActionResult.followUp(possibleAction, true))
                .orElse(ActionResult.none(true))
                .andThen(encounterActions(game));
    }

    protected boolean mustPayOtherMerchants() {
        return merchants.size() > 1;
    }

    private ActionResult encounterActions(Istanbul game) {
        var actions = new HashSet<PossibleAction>();

        if (governor) {
            actions.add(PossibleAction.optional(Action.Governor.class));
        }
        if (smuggler) {
            actions.add(PossibleAction.optional(Action.Smuggler.class));
        }

        var numberOfFamilyMembersToCatch = (int) familyMembersToCatch(game.getCurrentPlayer()).count();
        if (numberOfFamilyMembersToCatch > 0) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MAY_CATCH_FAMILY_MEMBER));

            actions.add(PossibleAction.repeat(numberOfFamilyMembersToCatch, numberOfFamilyMembersToCatch,
                    PossibleAction.choice(Set.of(
                            PossibleAction.optional(Action.CatchFamilyMemberForBonusCard.class),
                            PossibleAction.optional(Action.CatchFamilyMemberFor3Lira.class)))));
        }

        return !actions.isEmpty() ? ActionResult.followUp(PossibleAction.any(actions), true) : ActionResult.none(true);
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

    Player catchFamilyMember(Istanbul game) {
        var policeStation = game.getPoliceStation();

        var otherFamilyMember = familyMembersToCatch(game.getCurrentPlayer())
                .findAny()
                .orElseThrow(() -> new IstanbulException(IstanbulError.NO_FAMILY_MEMBER_TO_CATCH));

        takeFamilyMember(otherFamilyMember);
        policeStation.placeFamilyMember(game, otherFamilyMember);

        return otherFamilyMember;
    }

    Merchant getMerchant(PlayerColor color) {
        return merchants.stream()
                .filter(merchant -> merchant.getColor() == color)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Merchant not at place"));
    }

    JsonObjectBuilder serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("number", number)
                .add("merchants", serializer.fromCollection(merchants, Merchant::serialize))
                .add("assistants", serializer.fromIntegerMap(assistants, PlayerColor::name))
                .add("familyMembers", serializer.fromStrings(familyMembers, Player::getName))
                .add("governor", governor)
                .add("smuggler", smuggler);
    }

    static Place deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        var number = jsonObject.getInt("number");

        Place place;
        switch (number) {
            case 1:
                place = new Wainwright();
                break;
            case 2:
                place = new FabricWarehouse();
                break;
            case 3:
                place = new SpiceWarehouse();
                break;
            case 4:
                place = new FruitWarehouse();
                break;
            case 5:
                place = PostOffice.deserialize(jsonObject);
                break;
            case 6:
                place = Caravansary.deserialize(jsonObject);
                break;
            case 7:
                place = new Fountain();
                break;
            case 8:
                place = new BlackMarket();
                break;
            case 9:
                place = new TeaHouse();
                break;
            case LargeMarket.NUMBER:
            case SmallMarket.NUMBER:
                place = Market.deserialize(number, jsonObject);
                break;
            case 12:
                place = new PoliceStation();
                break;
            case 13:
                place = SultansPalace.deserialize(jsonObject);
                break;
            case SmallMosque.NUMBER:
            case GreatMosque.NUMBER:
                place = Mosque.deserialize(number, jsonObject);
                break;
            case 16:
                place = GemstoneDealer.deserialize(jsonObject);
                break;
            default:
                throw new IllegalArgumentException("Unknown place: " + number);
        }

        place.merchants.addAll(jsonObject.getJsonArray("merchants")
                .stream()
                .map(JsonValue::asJsonObject)
                .map(merchant -> Merchant.deserialize(playerMap, merchant))
                .collect(Collectors.toSet()));

        place.assistants.putAll(JsonDeserializer.forObject(jsonObject.getJsonObject("assistants")).asIntegerMap(PlayerColor::valueOf));

        place.familyMembers.addAll(jsonObject.getJsonArray("familyMembers").stream()
                .map(jsonValue -> (JsonString) jsonValue)
                .map(JsonString::getString)
                .map(playerMap::get)
                .collect(Collectors.toSet()));

        place.governor = jsonObject.getBoolean("governor");

        place.smuggler = jsonObject.getBoolean("smuggler");

        return place;
    }

    public static class Wainwright extends Place {

        Wainwright() {
            super(1);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.BuyWheelbarrowExtension.class));
        }

    }

    public static class FabricWarehouse extends Place {

        FabricWarehouse() {
            super(2);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.MaxFabric.class));
        }

    }

    public static class SpiceWarehouse extends Place {

        SpiceWarehouse() {
            super(3);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.MaxSpice.class));
        }

    }

    public static class FruitWarehouse extends Place {

        FruitWarehouse() {
            super(4);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.MaxFruit.class));
        }

    }

    public static class PostOffice extends Place {

        private final List<Integer> indicators;

        PostOffice() {
            this(new ArrayList<>(List.of(0, 0, 0, 0)));
        }

        private PostOffice(List<Integer> indicators) {
            super(5);
            this.indicators = indicators;
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.UsePostOffice.class));
        }

        ActionResult use(Istanbul game) {
            // row 1: fabric, 2 lira, blue, 2 lira
            // row 2: spice, 1 lira, fruit, 1 lira

            var currentPlayerState = game.currentPlayerState();
            var goodsType1 = indicators.get(0) == 1 ? GoodsType.SPICE : GoodsType.FABRIC;
            var goodsType2 = indicators.get(2) == 1 ? GoodsType.FRUIT : GoodsType.BLUE;
            var lira1 = indicators.get(1) == 1 ? 1 : 2;
            var lira2 = indicators.get(3) == 1 ? 1 : 2;

            currentPlayerState.addGoods(goodsType1, 1);
            currentPlayerState.addGoods(goodsType2, 1);
            currentPlayerState.gainLira(lira1 + lira2);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.GAIN_LIRA, Integer.toString(lira1 + lira2)));
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_GOODS, goodsType1.name(), "1"));
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_GOODS, goodsType2.name(), "1"));

            var leftMost = leftMostInTopRow();
            if (leftMost >= 0) {
                // Move from top row to bottom row
                indicators.set(leftMost, 1);
            } else {
                // Move all from bottom row back to top row
                IntStream.range(0, indicators.size()).forEach(index -> indicators.set(index, 0));
            }

            return ActionResult.none(true);
        }

        @Override
        JsonObjectBuilder serialize(JsonBuilderFactory factory) {
            var serializer = JsonSerializer.forFactory(factory);
            return super.serialize(factory)
                    .add("indicators", serializer.fromIntegers(indicators.stream()));
        }

        static PostOffice deserialize(JsonObject jsonObject) {
            return new PostOffice(jsonObject.getJsonArray("indicators").stream()
                    .map(jsonValue -> (JsonNumber) jsonValue)
                    .map(JsonNumber::intValue)
                    .collect(Collectors.toList()));
        }

        public List<Boolean> getIndicators() {
            return List.of(indicators.get(0) == 1, indicators.get(1) == 1, indicators.get(2) == 1, indicators.get(3) == 1);
        }

        private int leftMostInTopRow() {
            return indicators.indexOf(0);
        }
    }

    public static class Caravansary extends Place {

        @Getter
        private final List<BonusCard> discardPile;

        private Caravansary(List<BonusCard> discardPile) {
            super(6);
            this.discardPile = discardPile;
        }

        Caravansary() {
            this(new LinkedList<>());
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.whenThen(PossibleAction.optional(Action.Take2BonusCards.class),
                    PossibleAction.mandatory(Action.DiscardBonusCard.class), 0, 1));
        }

        @Override
        JsonObjectBuilder serialize(JsonBuilderFactory factory) {
            return super.serialize(factory)
                    .add("discardPile", JsonSerializer.forFactory(factory).fromStrings(discardPile, BonusCard::name));
        }

        static Caravansary deserialize(JsonObject jsonObject) {
            return new Caravansary(jsonObject.getJsonArray("discardPile").stream()
                    .map(jsonValue -> (JsonString) jsonValue)
                    .map(JsonString::getString)
                    .map(BonusCard::valueOf)
                    .collect(Collectors.toList()));
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

    public static class Fountain extends Place {

        Fountain() {
            super(7);
        }

        @Override
        ActionResult placeMerchant(Merchant merchant, Istanbul game) {
            super.placeMerchant(merchant, game);

            // Immediately activate and return place actions
            return placeActions(game);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
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

    public static class BlackMarket extends Place {

        BlackMarket() {
            super(8);
        }

        static void rollForBlueGoods(Istanbul game, @NonNull PlayerState playerState, @NonNull Random random) {
            var dice = random.nextInt(12) + 1;
            var amount = dice > 10 ? 3 : dice > 8 ? 2 : dice > 6 ? 1 : 0;

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.ROLL_FOR_BLUE, Integer.toString(dice), Integer.toString(amount)));

            playerState.addGoods(GoodsType.BLUE, amount);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.any(Set.of(
                    PossibleAction.choice(Set.of(
                            PossibleAction.optional(Action.Take1Fabric.class),
                            PossibleAction.optional(Action.Take1Spice.class),
                            PossibleAction.optional(Action.Take1Fruit.class))),
                    PossibleAction.optional(Action.RollForBlueGoods.class))));
        }
    }

    public static class TeaHouse extends Place {

        TeaHouse() {
            super(9);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.GuessAndRollForLira.class));
        }

        void guessAndRoll(@NonNull Istanbul game, int guess, @NonNull Random random) {
            var currentPlayer = game.getCurrentPlayer();

            game.fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.GUESSED, guess));

            var die1 = random.nextInt(6) + 1;
            var die2 = random.nextInt(6) + 1;

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
                    } else {
                        // Else reroll automatically
                        die1 = random.nextInt(6) + 1;
                        die2 = random.nextInt(6) + 1;

                        game.fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.ROLLED, die1, die2));

                        if (die1 + die2 >= guess) {
                            lira = guess;
                        }
                    }
                }
            }

            currentPlayerState.gainLira(lira);

            game.fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.GAIN_LIRA, Integer.toString(lira)));
        }
    }

    public static class Market extends Place {

        private final List<Map<GoodsType, Integer>> demands;
        private final List<Integer> rewards;

        protected Market(int number, List<Map<GoodsType, Integer>> demands, List<Integer> rewards) {
            super(number);
            this.demands = demands;
            this.rewards = rewards;
        }

        protected Market(int number,
                         Collection<Map<GoodsType, Integer>> demands,
                         List<Integer> rewards,
                         Random random) {
            this(number, new ArrayList<>(demands), rewards);

            Collections.shuffle(this.demands, random);
        }

        public Map<GoodsType, Integer> getDemand() {
            return demands.get(0);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.SellGoods.class));
        }

        @Override
        JsonObjectBuilder serialize(JsonBuilderFactory factory) {
            var serializer = JsonSerializer.forFactory(factory);
            return super.serialize(factory)
                    .add("demands", serializer.fromCollection(demands,
                            demand -> serializer.fromIntegerMap(demand, GoodsType::name)));
        }

        static Market deserialize(int number, JsonObject jsonObject) {
            var demands = jsonObject.getJsonArray("demands").stream()
                    .map(JsonValue::asJsonObject)
                    .map(JsonDeserializer::forObject)
                    .map(deserializer -> deserializer.asIntegerMap(GoodsType::valueOf))
                    .collect(Collectors.toList());

            switch (number) {
                case LargeMarket.NUMBER:
                    return new LargeMarket(demands);
                case SmallMarket.NUMBER:
                    return new SmallMarket(demands);
                default:
                    throw new IllegalArgumentException("Unknown market: " + number);
            }
        }

        void sellDemandGoods(Istanbul game, Map<GoodsType, Integer> goods) {
            var currentPlayerState = game.currentPlayerState();
            var demand = demands.get(0);

            var numberOfGoods = goods.entrySet().stream()
                    .mapToInt(entry -> {
                        var amount = currentPlayerState.removeGoods(entry.getKey(),
                                Math.min(entry.getValue(), demand.get(entry.getKey())));

                        game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.SELL_GOODS, Integer.toString(amount), entry.getKey().name()));

                        return amount;
                    })
                    .sum();

            sell(game, numberOfGoods);
        }

        protected void sell(Istanbul game, int numberOfGoods) {
            if (numberOfGoods > 0) {
                var reward = rewards.get(numberOfGoods - 1);

                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.GAIN_LIRA, Integer.toString(reward)));

                game.currentPlayerState().gainLira(reward);

                demands.add(demands.remove(0));
            }
        }

    }

    public static class SmallMarket extends Market {

        private static final int NUMBER = 11;
        private static final List<Integer> REWARDS = List.of(2, 5, 9, 14, 20);

        private SmallMarket(List<Map<GoodsType, Integer>> demands) {
            super(NUMBER, demands, REWARDS);
        }

        private SmallMarket(Random random) {
            super(NUMBER, List.of(
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 2, GoodsType.FRUIT, 1, GoodsType.BLUE, 1),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 2, GoodsType.FRUIT, 2, GoodsType.BLUE, 0),
                    Map.of(GoodsType.FABRIC, 0, GoodsType.SPICE, 2, GoodsType.FRUIT, 2, GoodsType.BLUE, 1),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 1, GoodsType.FRUIT, 2, GoodsType.BLUE, 1),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 3, GoodsType.FRUIT, 1, GoodsType.BLUE, 0)), REWARDS, random);
        }

        void sellAnyGoods(Istanbul game, Map<GoodsType, Integer> goods) {
            var currentPlayerState = game.currentPlayerState();

            var numberOfGoods = goods.entrySet().stream()
                    .mapToInt(entry -> {
                        game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.SELL_GOODS, entry.getValue().toString(), entry.getKey().name()));
                        return currentPlayerState.removeGoods(entry.getKey(), entry.getValue());
                    })
                    .sum();

            sell(game, numberOfGoods);
        }

        static SmallMarket randomize(@NonNull Random random) {
            return new SmallMarket(random);
        }
    }

    public static class LargeMarket extends Market {

        private static final int NUMBER = 10;
        private static final List<Integer> REWARDS = List.of(3, 7, 12, 18, 25);

        private LargeMarket(List<Map<GoodsType, Integer>> demands) {
            super(NUMBER, demands, REWARDS);
        }

        private LargeMarket(Random random) {
            super(NUMBER, List.of(
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 1, GoodsType.FRUIT, 1, GoodsType.BLUE, 2),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 1, GoodsType.FRUIT, 0, GoodsType.BLUE, 3),
                    Map.of(GoodsType.FABRIC, 2, GoodsType.SPICE, 1, GoodsType.FRUIT, 0, GoodsType.BLUE, 2),
                    Map.of(GoodsType.FABRIC, 1, GoodsType.SPICE, 0, GoodsType.FRUIT, 1, GoodsType.BLUE, 3),
                    Map.of(GoodsType.FABRIC, 2, GoodsType.SPICE, 0, GoodsType.FRUIT, 1, GoodsType.BLUE, 2)),
                    REWARDS, random);
        }

        static LargeMarket randomize(@NonNull Random random) {
            return new LargeMarket(random);
        }
    }

    public static class PoliceStation extends Place {

        PoliceStation() {
            super(12);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            if (getFamilyMembers().contains(game.getCurrentPlayer())) {
                return Optional.of(PossibleAction.optional(Action.SendFamilyMember.class));
            }
            return Optional.empty();
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
        static class MosqueTileStack {

            @Getter
            private final MosqueTile mosqueTile;
            private final List<Integer> goodsCounts;

            static MosqueTileStack forPlayerCount(@NonNull MosqueTile mosqueTile, int playerCount) {
                return new MosqueTileStack(mosqueTile, new LinkedList<>(initialGoodsCounts(playerCount)));
            }

            static MosqueTileStack withGoodsCounts(@NonNull MosqueTile mosqueTile, @NonNull List<Integer> goodsCounts) {
                return new MosqueTileStack(mosqueTile, goodsCounts);
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

        Mosque(int number, @NonNull MosqueTileStack a, @NonNull MosqueTileStack b) {
            super(number);
            this.a = a;
            this.b = b;
        }

        @Override
        JsonObjectBuilder serialize(JsonBuilderFactory factory) {
            var serializer = JsonSerializer.forFactory(factory);
            return super.serialize(factory)
                    .add("a", serializer.fromIntegers(a.goodsCounts.stream()))
                    .add("b", serializer.fromIntegers(b.goodsCounts.stream()));
        }

        static Mosque deserialize(int number, JsonObject jsonObject) {
            var a = jsonObject.getJsonArray("a").stream()
                    .map(jsonValue -> (JsonNumber) jsonValue)
                    .map(JsonNumber::intValue)
                    .collect(Collectors.toList());

            var b = jsonObject.getJsonArray("b").stream()
                    .map(jsonValue -> (JsonNumber) jsonValue)
                    .map(JsonNumber::intValue)
                    .collect(Collectors.toList());

            switch (number) {
                case SmallMosque.NUMBER:
                    return SmallMosque.withGoodsCounts(a, b);
                case GreatMosque.NUMBER:
                    return GreatMosque.withGoodsCounts(a, b);
                default:
                    throw new IllegalArgumentException("Unknown mosque: " + number);
            }
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
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

        ActionResult takeMosqueTile(@NonNull MosqueTile mosqueTile, @NonNull Istanbul game) {
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

    public static class SmallMosque extends Mosque {

        private static final int NUMBER = 14;
        private static final MosqueTile A = MosqueTile.TURN_OR_REROLL_DICE;
        private static final MosqueTile B = MosqueTile.PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD;

        private SmallMosque(MosqueTileStack a, MosqueTileStack b) {
            super(NUMBER, a, b);
        }

        static SmallMosque forPlayerCount(int playerCount) {
            return new SmallMosque(
                    MosqueTileStack.forPlayerCount(A, playerCount),
                    MosqueTileStack.forPlayerCount(B, playerCount));
        }

        static SmallMosque withGoodsCounts(List<Integer> a, List<Integer> b) {
            return new SmallMosque(
                    MosqueTileStack.withGoodsCounts(A, a),
                    MosqueTileStack.withGoodsCounts(B, b));
        }
    }

    public static class GreatMosque extends Mosque {

        private static final int NUMBER = 15;
        private static final MosqueTile A = MosqueTile.PAY_2_LIRA_TO_RETURN_ASSISTANT;
        private static final MosqueTile B = MosqueTile.EXTRA_ASSISTANT;

        private GreatMosque(MosqueTileStack a, MosqueTileStack b) {
            super(NUMBER, a, b);
        }

        static GreatMosque forPlayerCount(int playerCount) {
            return new GreatMosque(
                    MosqueTileStack.forPlayerCount(A, playerCount),
                    MosqueTileStack.forPlayerCount(B, playerCount));
        }

        static GreatMosque withGoodsCounts(List<Integer> a, List<Integer> b) {
            return new GreatMosque(
                    MosqueTileStack.withGoodsCounts(A, a),
                    MosqueTileStack.withGoodsCounts(B, b));
        }
    }


    public static class SultansPalace extends Place {

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

        static SultansPalace deserialize(JsonObject jsonObject) {
            return new SultansPalace(jsonObject.getInt("uncovered"));
        }

        @Override
        JsonObjectBuilder serialize(JsonBuilderFactory factory) {
            return super.serialize(factory)
                    .add("uncovered", uncovered);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
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
                            PossibleAction.optional(Action.Pay1Fabric.class),
                            PossibleAction.optional(Action.Pay1Fruit.class),
                            PossibleAction.optional(Action.Pay1Spice.class),
                            PossibleAction.optional(Action.Pay1Blue.class)))), true);
        }

        private boolean hasEnoughGoods(PlayerState playerState, Map<GoodsType, Long> requiredGoodsByType) {
            return requiredGoodsByType.entrySet().stream()
                    .allMatch(goods -> playerState.hasAtLeastGoods(goods.getKey(), goods.getValue().intValue()));
        }
    }

    public static class GemstoneDealer extends Place {

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

        static GemstoneDealer deserialize(JsonObject jsonObject) {
            return new GemstoneDealer(jsonObject.getInt("cost"));
        }

        @Override
        JsonObjectBuilder serialize(JsonBuilderFactory factory) {
            return super.serialize(factory)
                    .add("cost", cost);
        }

        @Override
        protected Optional<PossibleAction> getPossibleAction(Istanbul game) {
            return Optional.of(PossibleAction.optional(Action.BuyRuby.class));
        }

        void buy(Istanbul game) {
            if (cost > 23) {
                throw new IstanbulException(IstanbulError.NO_RUBY_AVAILABLE);
            }

            var playerState = game.currentPlayerState();

            playerState.payLira(cost);
            playerState.gainRubies(1);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.BUY_RUBY, Integer.toString(cost)));

            cost++;
        }
    }

}
