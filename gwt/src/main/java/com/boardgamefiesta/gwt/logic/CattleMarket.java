package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
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
            3, Set.of(
                    Cost.single(6, 1),
                    Cost.single(3, 2),
                    Cost.pair(5, 3),
                    // 2 singles:
                    Cost.pair(12, 2),
                    Cost.pair(9, 3),
                    Cost.pair(6, 4)
            ),
            4, Set.of(
                    Cost.single(12, 1),
                    Cost.single(6, 3),
                    Cost.pair(8, 5),
                    // 2 singles:
                    Cost.pair(12, 6),
                    Cost.pair(24, 2),
                    Cost.pair(18, 4)
            ),
            5, Set.of(
                    Cost.single(12, 2),
                    Cost.single(6, 4),
                    // 2 singles:
                    Cost.pair(18, 6),
                    Cost.pair(24, 4)
            ));

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

    public Stream<PossibleBuy> possibleBuys(int numberOfCowboys, int balance) {
        if (numberOfCowboys == 0 || balance < 3) {
            return Stream.empty();
        }

        return market.stream()
                .map(Card.CattleCard::getType)
                .mapToInt(CattleType::getValue)
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(count -> count.getValue() > 0)
                .flatMap(count -> COSTS.get(count.getKey()).stream()
                        .filter(cost -> cost.getCowboys() <= numberOfCowboys)
                        .filter(cost -> cost.getDollars() <= balance)
                        .filter(cost -> !cost.isPair() || count.getValue() >= 2)
                        .map(cost -> new PossibleBuy(count.getKey(), cost.isPair(), cost.getDollars(), cost.getCowboys())));
    }

    public int getDrawStackSize() {
        return drawStack.size();
    }

    @Value
    public static class Cost {
        int dollars;
        int cowboys;
        boolean pair;

        static Cost pair(int dollars, int cowboys) {
            return new Cost(dollars, cowboys, true);
        }

        static Cost single(int dollars, int cowboys) {
            return new Cost(dollars, cowboys, false);
        }
    }

    Cost buy(@NonNull Card.CattleCard card, Card.CattleCard secondCard, int cowboys, int dollars) {
        if (secondCard != null && (secondCard == card || secondCard.getType().getValue() != card.getType().getValue())) {
            throw new GWTException(GWTError.NOT_PAIR);
        }

        if (!market.contains(card) || (secondCard != null && !market.contains(secondCard))) {
            throw new GWTException(GWTError.CATTLE_CARD_NOT_AVAILABLE);
        }

        var result = COSTS.get(card.getType().getValue()).stream()
                .filter(cost -> cost.isPair() == (secondCard != null))
                .filter(cost -> cost.getCowboys() == cowboys)
                .filter(cost -> cost.getDollars() == dollars)
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.CANNOT_PERFORM_ACTION));

        market.remove(card);
        if (secondCard != null) {
            market.remove(secondCard);
        }

        return result;
    }

    void fillUp() {
        while (market.size() < limit && !drawStack.isEmpty()) {
            draw();
        }
    }

    void draw() {
        if (!drawStack.isEmpty()) {
            market.add(drawStack.poll());
        }
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
        int dollars;
        int cowboys;
    }

}
