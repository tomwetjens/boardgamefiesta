package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CattleMarket {

    private final int limit;
    private final Queue<Card.CattleCard> drawStack;
    private final Set<Card.CattleCard> market;

    private static final Map<Integer, Set<Cost>> COSTS = Map.of(
            3, Set.of(new Cost(6, 1, false), new Cost(3, 2, false), new Cost(5, 3, true)),
            4, Set.of(new Cost(12, 1, false), new Cost(6, 3, false), new Cost(8, 5, true)),
            5, Set.of(new Cost(12, 2, false), new Cost(6, 4, false), new Cost(18, 6, true)));

    CattleMarket(int playerCount, Random random) {
        this(playerCount, createDrawStack(random), new HashSet<>());
        fillUp();
    }

    CattleMarket(int playerCount, Queue<Card.CattleCard> drawStack, Set<Card.CattleCard> market) {
        this.limit = playerCount == 2 ? 7 : playerCount == 3 ? 10 : 13;
        this.drawStack = drawStack;
        this.market = market;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("drawStack", serializer.fromCollection(drawStack, Card.CattleCard::serialize))
                .add("market", serializer.fromCollection(market, Card.CattleCard::serialize))
                .build();
    }

    static CattleMarket deserialize(int playerCount, JsonObject jsonObject) {
        return new CattleMarket(playerCount,
                jsonObject.getJsonArray("drawStack").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Card.CattleCard::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                jsonObject.getJsonArray("market").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Card.CattleCard::deserialize)
                        .collect(Collectors.toSet()));
    }

    public Set<Card.CattleCard> getMarket() {
        return Collections.unmodifiableSet(market);
    }

    /**
     * Calculates the options for buying a single card or a pair of cards.
     *
     * @param breedingValue   The breeding value to buy.
     * @param pair            Buying 1 or a pair.
     * @param numberOfCowboys The number of cowboys available.
     * @param balance         The balance the player has.
     * @param costPreference  The cost preference.
     * @return Cost for buying a single card or a pair of cards.
     */
    public static Cost cost(int breedingValue, boolean pair, int numberOfCowboys, int balance, CostPreference costPreference) {
        if (numberOfCowboys == 0) {
            throw new GWTException(GWTError.NOT_ENOUGH_COWBOYS);
        }

        var costs = COSTS.get(breedingValue).stream()
                .filter(cost -> !cost.isPair() || pair)
                .map(cost -> pair && !cost.isPair() ? cost.twice() : cost)
                .filter(cost -> cost.getCowboys() <= numberOfCowboys)
                .collect(Collectors.toSet());

        if (costs.isEmpty()) {
            throw new GWTException(GWTError.NOT_ENOUGH_COWBOYS);
        }

        return costs.stream()
                .filter(cost -> cost.getDollars() <= balance)
                .min(costPreference.getComparator())
                .orElseThrow(() -> new GWTException(GWTError.NOT_ENOUGH_BALANCE_TO_PAY));
    }


    public Stream<PossibleBuy> possibleBuys(int numberOfCowboys, int balance) {
        if (numberOfCowboys == 0 || balance < 3) {
            return Stream.empty();
        }

        var counts = market.stream()
                .map(Card.CattleCard::getType)
                .mapToInt(CattleType::getValue)
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return COSTS.entrySet().stream()
                .filter(entry -> counts.containsKey(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream()
                        .filter(cost -> cost.getCowboys() <= numberOfCowboys)
                        .filter(cost -> cost.getDollars() <= balance)
                        .filter(cost -> !cost.isPair() || counts.getOrDefault(entry.getKey(), 0L) >= 2)
                        .map(cost -> new PossibleBuy(entry.getKey(), cost.isPair(), cost.getDollars(), cost.getCowboys())));
    }

    public int getDrawStackSize() {
        return drawStack.size();
    }

    @Value
    public static class Cost {
        int dollars;
        int cowboys;
        boolean pair;

        public Cost twice() {
            return new Cost(dollars * 2, cowboys * 2, true);
        }
    }

    @AllArgsConstructor
    public enum CostPreference {

        CHEAPEST(Comparator.comparingInt(Cost::getDollars)),
        LESS_COWBOYS(Comparator.comparingInt(Cost::getCowboys));

        @Getter
        private final Comparator<Cost> comparator;

    }

    Cost buy(@NonNull Card.CattleCard card, Card.CattleCard secondCard, int numberOfCowboys, int balance, CostPreference costPreference) {
        if (secondCard != null && (secondCard == card || secondCard.getType().getValue() != card.getType().getValue())) {
            throw new GWTException(GWTError.NOT_PAIR);
        }

        if (!market.contains(card) || (secondCard != null && !market.contains(secondCard))) {
            throw new GWTException(GWTError.CATTLE_CARD_NOT_AVAILABLE);
        }

        Cost cost = cost(card.getType().getValue(), secondCard != null, numberOfCowboys, balance, costPreference);

        market.remove(card);
        if (secondCard != null) {
            market.remove(secondCard);
        }

        return cost;
    }

    void fillUp() {
        while (market.size() < limit) {
            draw();
        }
    }

    void draw() {
        market.add(drawStack.poll());
    }

    private static Queue<Card.CattleCard> createDrawStack(Random random) {
        List<Card.CattleCard> cards = createSet();

        Collections.shuffle(cards, random);

        return new LinkedList<>(cards);
    }

    private static List<Card.CattleCard> createSet() {
        List<Card.CattleCard> cards = new ArrayList<>(36);
        IntStream.range(0, 7).mapToObj(i -> new Card.CattleCard(CattleType.HOLSTEIN, 1)).forEach(cards::add);
        IntStream.range(0, 7).mapToObj(i -> new Card.CattleCard(CattleType.BROWN_SWISS, 2)).forEach(cards::add);
        IntStream.range(0, 7).mapToObj(i -> new Card.CattleCard(CattleType.AYRSHIRE, 3)).forEach(cards::add);
        IntStream.range(0, 3).mapToObj(i -> new Card.CattleCard(CattleType.WEST_HIGHLAND, 3)).forEach(cards::add);
        IntStream.range(0, 3).mapToObj(i -> new Card.CattleCard(CattleType.WEST_HIGHLAND, 4)).forEach(cards::add);
        IntStream.range(0, 3).mapToObj(i -> new Card.CattleCard(CattleType.WEST_HIGHLAND, 5)).forEach(cards::add);
        IntStream.range(0, 2).mapToObj(i -> new Card.CattleCard(CattleType.TEXAS_LONGHORN, 5)).forEach(cards::add);
        IntStream.range(0, 2).mapToObj(i -> new Card.CattleCard(CattleType.TEXAS_LONGHORN, 6)).forEach(cards::add);
        IntStream.range(0, 2).mapToObj(i -> new Card.CattleCard(CattleType.TEXAS_LONGHORN, 7)).forEach(cards::add);
        return cards;
    }

    @Value
    public static class PossibleBuy {
        int breedingValue;
        boolean pair;
        int cost;
        int cowboysNeeded;
    }

}
