package com.tomsboardgames.gwt;

import com.tomsboardgames.json.JsonSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CattleMarket {

    private final int limit;
    private final Queue<Card.CattleCard> drawStack;
    private final Set<Card.CattleCard> market;

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
     * Calculates the options the player has for buying a single card or a pair of cards from the market.
     *
     * @param numberOfCowboys Number of cowboys the player has available.
     * @param balance         The balance the player has.
     * @return Possibilities for buying a single card or a pair.
     */
    public Set<PossibleBuy> possibleBuys(int numberOfCowboys, int balance) {
        return possibleBuys(new ArrayList<>(market), numberOfCowboys, balance);
    }

    private static Set<PossibleBuy> possibleBuys(List<Card.CattleCard> market, int numberOfCowboys, int balance) {
        if (numberOfCowboys == 0) {
            return Collections.emptySet();
        }

        Stream<PossibleBuy> result = Stream.empty();
        if (numberOfCowboys >= 5) {
            result = Stream.concat(result, pairPossibleBuys(market, 4, 8, 5));
        }
        if (numberOfCowboys >= 4) {
            result = Stream.concat(result, singlePossibleBuys(market, 5, 6, 4));
        }
        if (numberOfCowboys >= 3) {
            result = Stream.concat(result, Stream.concat(
                    pairPossibleBuys(market, 3, 5, 3),
                    singlePossibleBuys(market, 4, 6, 3)));
        }
        if (numberOfCowboys >= 2) {
            result = Stream.concat(result, Stream.concat(
                    singlePossibleBuys(market, 3, 3, 2),
                    singlePossibleBuys(market, 5, 12, 2)));
        }
        if (numberOfCowboys >= 1) {
            result = Stream.concat(result, Stream.concat(
                    singlePossibleBuys(market, 3, 6, 1),
                    singlePossibleBuys(market, 4, 12, 1)));
        }

        return result.filter(possibleBuy -> possibleBuy.cost <= balance).collect(Collectors.toSet());
    }

    private static Stream<PossibleBuy> singlePossibleBuys(List<Card.CattleCard> market, int breedingValue, int cost, int cowboysNeeded) {
        return market.stream()
                .filter(cattleCard -> cattleCard.getType().getValue() == breedingValue)
                .map(cattleCard -> new PossibleBuy(Collections.singletonList(cattleCard.getType().getValue()), cost, cowboysNeeded));
    }

    private static Stream<PossibleBuy> pairPossibleBuys(List<Card.CattleCard> market, int breedingValue, int cost, int cowboysNeeded) {
        return findPairs(market, breedingValue)
                .limit(1) // Since the result only indicates the breeding value, there is no need to return each pair
                .map(pair -> new PossibleBuy(Arrays.asList(pair.a.getType().getValue(), pair.b.getType().getValue()), cost, cowboysNeeded));
    }


    private static Stream<Pair<Card.CattleCard>> findPairs(List<Card.CattleCard> market, int breedingValue) {
        return market.stream()
                .filter(cattleCard -> cattleCard.getType().getValue() == breedingValue)
                .flatMap(a -> market.stream() // Find pair
                        .dropWhile(b -> b != a) // Starting from card A to find pair, to prevent a single pair appearing twice in the output
                        .skip(1) // Skip card A itself
                        .filter(cattleCard -> cattleCard.getType().getValue() == a.getType().getValue()) // Find pair of matching breeding value
                        .map(b -> new Pair<>(a, b)));
    }

    /**
     * Calculates the minimum amount of dollars needed to buy specified cards with the specified number of cowboys.
     *
     * @param cattleCards     The cards to buy.
     * @param numberOfCowboys The number of cowboys available.
     * @return Minimum amount of dollars.
     */
    public int cost(Collection<Card.CattleCard> cattleCards, int numberOfCowboys) {
        return cost(cattleCards, numberOfCowboys, CostPreference.CHEAPER_COST).getDollars();
    }

    /**
     * Calculates the minimum cost needed to buy the specified cards with the specified number of cowboys, based on the cost preference.
     *
     * @param cattleCards     The cards to buy.
     * @param numberOfCowboys The number of cowboys available.
     * @param preference      The cost preference: either calculate the minimum amount of dollars or the minimum amount of cowboys.
     * @return The minimum cost based on the specified cost preference.
     */
    public Cost cost(Collection<Card.CattleCard> cattleCards, int numberOfCowboys, CostPreference preference) {
        return cost(new ArrayList<>(market), cattleCards, numberOfCowboys, preference);
    }

    public int getDrawStackSize() {
        return drawStack.size();
    }

    @Value
    public static class Cost {
        int dollars;
        int cowboys;
    }

    @AllArgsConstructor
    public enum CostPreference {

        CHEAPER_COST(Comparator.comparingInt(Cost::getDollars)),
        LESS_COWBOYS(Comparator.comparingInt(Cost::getCowboys));

        @Getter
        private final Comparator<Cost> comparator;
    }

    private static Cost cost(List<Card.CattleCard> cattleCardsRemaining, Collection<Card.CattleCard> cattleCards, int numberOfCowboys, CostPreference preference) {
        return costIfEnoughCowboys(cattleCardsRemaining, cattleCards, numberOfCowboys, preference)
                .orElseThrow(() -> new GWTException(GWTError.NOT_ENOUGH_COWBOYS));
    }

    private static Optional<Cost> costIfEnoughCowboys(List<Card.CattleCard> cattleCardsRemaining, Collection<Card.CattleCard> cattleCards, int numberOfCowboys, CostPreference preference) {
        Map<Integer, List<Card.CattleCard>> groupedByBreedingValue = cattleCards.stream()
                .distinct()
                .collect(Collectors.groupingBy(cattleCard -> cattleCard.getType().getValue()));

        return groupedByBreedingValue.keySet().stream()
                .findFirst()
                .flatMap(breedingValue -> {
                    List<Card.CattleCard> cardsOfSameBreedingValue = groupedByBreedingValue.get(breedingValue);
                    boolean considerPair = cardsOfSameBreedingValue.size() > 1;

                    Set<PossibleBuy> possibleBuys = possible(cattleCardsRemaining, breedingValue, considerPair, numberOfCowboys).collect(Collectors.toSet());

                    return possibleBuys.stream()
                            .flatMap(possibleBuy -> {
                                boolean moreCardsToBuy = cattleCards.size() > possibleBuy.getBreedingValues().size();

                                if (moreCardsToBuy) {
                                    List<Card.CattleCard> cardsBuying = cardsOfSameBreedingValue.subList(0, possibleBuy.getBreedingValues().size());
                                    List<Card.CattleCard> marketAfterBuy = removeAll(cattleCardsRemaining, cardsBuying);
                                    List<Card.CattleCard> cardsRemainingToBuy = removeAll(cattleCards, cardsBuying);
                                    int cowboysRemaining = numberOfCowboys - possibleBuy.getCowboysNeeded();

                                    Optional<Cost> cost = costIfEnoughCowboys(marketAfterBuy, cardsRemainingToBuy, cowboysRemaining, preference)
                                            .map(furtherCost -> new Cost(furtherCost.getDollars() + possibleBuy.getCost(), furtherCost.getCowboys() + possibleBuy.getCowboysNeeded()));

                                    return cost.stream();
                                }

                                return Stream.of(new Cost(possibleBuy.getCost(), possibleBuy.getCowboysNeeded()));
                            })
                            .min(preference.getComparator());
                });
    }

    private static List<Card.CattleCard> removeAll(Collection<Card.CattleCard> market, Collection<Card.CattleCard> buy) {
        if (!market.containsAll(buy)) {
            throw new GWTException(GWTError.CATTLE_CARD_NOT_AVAILABLE);
        }
        return market.stream().filter(e -> !buy.contains(e)).collect(Collectors.toList());
    }

    private static Stream<PossibleBuy> possible(List<Card.CattleCard> available, Integer breedingValue, boolean considerPair, int numberOfCowboys) {
        Set<PossibleBuy> possibleBuys = possibleBuys(available, Integer.MAX_VALUE, Integer.MAX_VALUE);

        Set<PossibleBuy> possibleSingles = possibleBuys.stream()
                .filter(pb -> pb.getBreedingValues().size() == 1)
                .filter(pb -> pb.getBreedingValues().get(0).equals(breedingValue))
                .collect(Collectors.toSet());

        if (possibleSingles.isEmpty()) {
            throw new GWTException(GWTError.NOT_ENOUGH_CATTLE_CARDS_OF_BREEDING_VALUE_AVAILABLE);
        }

        Set<PossibleBuy> possiblePairs = Collections.emptySet();

        if (considerPair) {
            possiblePairs = possibleBuys.stream()
                    .filter(pb -> pb.getBreedingValues().size() == 2)
                    .filter(pb -> pb.getBreedingValues().get(0).equals(breedingValue) && pb.getBreedingValues().get(1).equals(breedingValue))
                    .filter(pb -> pb.getCowboysNeeded() <= numberOfCowboys)
                    .collect(Collectors.toSet());
        }

        return Stream.concat(possiblePairs.stream(), possibleSingles.stream())
                .filter(pb -> pb.getCowboysNeeded() <= numberOfCowboys);
    }

    int buy(Set<Card.CattleCard> cattleCards, int numberOfCowboys) {
        // Assume player wants cheaper instead of less cowboys
        Cost cost = cost(cattleCards, numberOfCowboys, CostPreference.CHEAPER_COST);

        market.removeAll(cattleCards);

        return numberOfCowboys - cost.getCowboys();
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
        List<Integer> breedingValues;
        int cost;
        int cowboysNeeded;

        public PossibleBuy(List<Integer> breedingValues, int cost, int cowboysNeeded) {
            this.breedingValues = new ArrayList<>(breedingValues);

            // [3,3,4] and [4,3,3] should be considered equal, however duplicates are allowed
            // Therefore sort the list
            this.breedingValues.sort(Integer::compareTo);

            this.cost = cost;
            this.cowboysNeeded = cowboysNeeded;
        }
    }

    @Value
    private static class Pair<T> {
        T a;
        T b;
    }
}
